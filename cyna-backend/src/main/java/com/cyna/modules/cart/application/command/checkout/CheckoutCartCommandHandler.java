package com.cyna.modules.cart.application.command.checkout;

import com.cyna.modules.cart.application.model.CheckoutCartReadModel;
import com.cyna.modules.cart.application.model.CartTotalsReadModel;
import com.cyna.modules.cart.application.service.CartAccessService;
import com.cyna.modules.cart.application.service.CartReadModelService;
import com.cyna.modules.cart.domain.event.CartCheckedOut;
import com.cyna.modules.cart.domain.model.Cart;
import com.cyna.modules.cart.domain.model.CartLine;
import com.cyna.modules.cart.domain.model.CartStatus;
import com.cyna.modules.cart.domain.repository.CartRepository;
import com.cyna.modules.order.application.api.OrderCommandApi;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class CheckoutCartCommandHandler implements CommandHandler<CheckoutCartCommand, CheckoutCartReadModel> {

    private final CartAccessService cartAccessService;
    private final CartReadModelService cartReadModelService;
    private final CartRepository cartRepository;
    private final OrderCommandApi orderCommandApi;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public CheckoutCartCommandHandler(CartAccessService cartAccessService,
                                      CartReadModelService cartReadModelService,
                                      CartRepository cartRepository,
                                      OrderCommandApi orderCommandApi,
                                      DomainEventPublisher eventPublisher,
                                      TransactionRunner transactionRunner) {
        this.cartAccessService = cartAccessService;
        this.cartReadModelService = cartReadModelService;
        this.cartRepository = cartRepository;
        this.orderCommandApi = orderCommandApi;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<CheckoutCartReadModel> handle(CheckoutCartCommand command) {
        return transactionRunner.runReturning(() -> {
            if (command.userId() == null) {
                return Result.failure("Authenticated user is required");
            }

            Result<Cart> cartResult = cartAccessService.getRequiredActiveCart(command.userId());
            if (cartResult.isFailure()) {
                if ("Active cart not found".equals(cartResult.getError())) {
                    var latestCartOpt = cartRepository.findLatestByUserId(command.userId());
                    if (latestCartOpt.isPresent() && latestCartOpt.get().getStatus() == CartStatus.CHECKED_OUT) {
                        return Result.failure("Cart is already checked out");
                    }
                }
                return Result.failure(cartResult.getError());
            }

            Cart cart = cartResult.getValue();
            if (cart.getLines().isEmpty()) {
                return Result.failure("Cart is empty");
            }

            Result<CartTotalsReadModel> totalsResult = cartReadModelService.calculateTotals(cart);
            if (totalsResult.isFailure()) {
                return Result.failure(totalsResult.getError());
            }

            Result<Cart> checkedOutResult = cart.markCheckedOut();
            if (checkedOutResult.isFailure()) {
                return Result.failure(checkedOutResult.getError());
            }

            Cart checkedOut = checkedOutResult.getValue();
            cartAccessService.save(checkedOut);

            // Same-transaction chain to keep cart↔order atomic — mirrors the
            // payment→subscription handshake in FinalizePaymentCommandHandler.
            // billingAddress is null: cart doesn't yet capture it (collected on
            // the checkout/payment step from the PaymentMethod). Stripe Tax
            // derives the jurisdiction from the PaymentMethod billing details
            // in updateCustomerTaxLocation. A future cart enhancement may
            // collect the address upfront and pass it here.
            Result<UUID> orderResult = orderCommandApi.createOrderFromCart(
                    command.userId(),
                    toOrderLines(checkedOut.getLines()),
                    null
            );
            if (orderResult.isFailure()) {
                return Result.failure(orderResult.getError());
            }
            UUID orderId = orderResult.getValue();

            eventPublisher.publish(new CartCheckedOut(
                    checkedOut.getId(),
                    command.userId(),
                    orderId,
                    toEventLines(checkedOut.getLines()),
                    Instant.now()
            ));

            CartTotalsReadModel totals = totalsResult.getValue();
            return Result.success(new CheckoutCartReadModel(
                    checkedOut.getId(),
                    orderId,
                    totals.subtotalHt(),
                    totals.currency()
            ));
        });
    }

    private static List<OrderCommandApi.CartLineRequest> toOrderLines(List<CartLine> lines) {
        return lines.stream()
                .map(l -> new OrderCommandApi.CartLineRequest(
                        l.getProductId(),
                        l.getBillingCycle(),
                        l.getQuantity()))
                .toList();
    }

    private static List<CartCheckedOut.Line> toEventLines(List<CartLine> lines) {
        return lines.stream()
                .map(l -> new CartCheckedOut.Line(
                        l.getProductId(),
                        l.getProductName(),
                        l.getProductCategory(),
                        l.getBillingCycle(),
                        l.getQuantity()))
                .toList();
    }
}
