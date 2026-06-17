package com.cyna.modules.order.application.command.create;

import com.cyna.modules.order.domain.model.BillingAddress;
import com.cyna.modules.order.domain.model.Order;
import com.cyna.modules.order.domain.model.OrderLine;
import com.cyna.modules.order.domain.repository.OrderRepository;
import com.cyna.modules.product.application.api.ProductInfo;
import com.cyna.modules.product.application.api.ProductQueryApi;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand, UUID> {

    private final OrderRepository orderRepository;
    private final ProductQueryApi productQueryApi;
    private final TransactionRunner transactionRunner;
    private final DomainEventPublisher eventPublisher;

    public CreateOrderCommandHandler(OrderRepository orderRepository,
                                     ProductQueryApi productQueryApi,
                                     TransactionRunner transactionRunner,
                                     DomainEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.productQueryApi = productQueryApi;
        this.transactionRunner = transactionRunner;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<UUID> handle(CreateOrderCommand command) {
        return transactionRunner.runReturning(() -> {
            if (command.lines() == null || command.lines().isEmpty()) {
                return Result.failure("Order must have at least one line");
            }

            List<UUID> productIds = command.lines().stream()
                    .map(CreateOrderCommand.CreateOrderLine::productId)
                    .distinct()
                    .toList();

            List<ProductInfo> products = productQueryApi.getByIds(productIds);
            Map<UUID, ProductInfo> byId = products.stream()
                    .collect(Collectors.toMap(ProductInfo::id, p -> p));

            List<OrderLine> lines = new ArrayList<>();
            for (CreateOrderCommand.CreateOrderLine line : command.lines()) {
                ProductInfo product = byId.get(line.productId());
                if (product == null) {
                    return Result.failure("Product not found: " + line.productId());
                }
                if (!product.isPublished()) {
                    return Result.failure("Product is not available: " + line.productId());
                }

                Money unitPrice = line.billingCycle() == BillingCycle.MONTHLY
                        ? Money.of(product.monthlyPrice(), product.currency())
                        : Money.of(product.annualPrice(), product.currency());

                lines.add(OrderLine.create(
                        product.id(),
                        product.name(),
                        product.categoryName(),
                        line.billingCycle(),
                        line.quantity(),
                        unitPrice,
                        product.freeTrialDays()
                ));
            }

            BillingAddress domainAddress = null;
            if (command.billingAddress() != null) {
                var ba = command.billingAddress();
                domainAddress = new BillingAddress(ba.line1(), ba.city(), ba.zipCode(), ba.countryCode());
            }

            Order order = Order.create(command.userId(), lines, domainAddress);
            orderRepository.save(order);
            eventPublisher.publishAll(order.getDomainEvents());
            order.clearDomainEvents();

            return Result.success(order.getId());
        });
    }
}
