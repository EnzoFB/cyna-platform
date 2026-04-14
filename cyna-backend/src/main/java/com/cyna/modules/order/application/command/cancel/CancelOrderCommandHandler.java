package com.cyna.modules.order.application.command.cancel;

import com.cyna.modules.order.domain.model.Order;
import com.cyna.modules.order.domain.repository.OrderRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CancelOrderCommandHandler implements CommandHandler<CancelOrderCommand, UUID> {

    private final OrderRepository orderRepository;
    private final TransactionRunner transactionRunner;
    private final DomainEventPublisher eventPublisher;

    public CancelOrderCommandHandler(OrderRepository orderRepository,
                                     TransactionRunner transactionRunner,
                                     DomainEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.transactionRunner = transactionRunner;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<UUID> handle(CancelOrderCommand command) {
        return transactionRunner.runReturning(() -> {
            Order order = orderRepository.findById(command.orderId()).orElse(null);
            if (order == null) {
                return Result.failure("Order not found: " + command.orderId());
            }
            if (!order.getUserId().equals(command.userId())) {
                return Result.failure("Access denied");
            }

            Result<Order> cancelledResult = order.cancel(command.reason());
            if (cancelledResult.isFailure()) {
                return Result.failure(cancelledResult.getError());
            }

            Order cancelled = cancelledResult.getValue();
            orderRepository.save(cancelled);
            eventPublisher.publishAll(cancelled.getDomainEvents());
            cancelled.clearDomainEvents();

            return Result.success(cancelled.getId());
        });
    }
}
