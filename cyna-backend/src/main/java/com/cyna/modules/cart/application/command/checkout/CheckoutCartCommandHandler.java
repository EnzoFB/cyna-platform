package com.cyna.modules.cart.application.command.checkout;

import com.cyna.modules.cart.application.model.CheckoutCartReadModel;
import com.cyna.modules.cart.application.model.CartTotalsReadModel;
import com.cyna.modules.cart.application.service.CartAccessService;
import com.cyna.modules.cart.application.service.CartReadModelService;
import com.cyna.modules.cart.domain.model.Cart;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class CheckoutCartCommandHandler implements CommandHandler<CheckoutCartCommand, CheckoutCartReadModel> {

    private final CartAccessService cartAccessService;
    private final CartReadModelService cartReadModelService;
    private final TransactionRunner transactionRunner;

    public CheckoutCartCommandHandler(CartAccessService cartAccessService,
                                      CartReadModelService cartReadModelService,
                                      TransactionRunner transactionRunner) {
        this.cartAccessService = cartAccessService;
        this.cartReadModelService = cartReadModelService;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<CheckoutCartReadModel> handle(CheckoutCartCommand command) {
        return transactionRunner.runReturning(() -> {
            if (command.userId() == null) {
                return Result.failure("Authenticated user is required");
            }

            Result<Cart> cartResult = cartAccessService.getRequiredActiveCart(command.userId(), null);
            if (cartResult.isFailure()) {
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

            CartTotalsReadModel totals = totalsResult.getValue();
            return Result.success(new CheckoutCartReadModel(
                    checkedOut.getId(),
                    totals.subtotalHt(),
                    totals.vatAmount(),
                    totals.totalTtc(),
                    totals.currency()
            ));
        });
    }
}
