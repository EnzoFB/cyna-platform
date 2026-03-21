package com.cyna.modules.cart.application.command.updatebillingcycle;

import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.modules.cart.application.service.CartAccessService;
import com.cyna.modules.cart.application.service.CartReadModelService;
import com.cyna.modules.cart.domain.model.Cart;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class UpdateCartLineBillingCycleCommandHandler implements CommandHandler<UpdateCartLineBillingCycleCommand, CartReadModel> {

    private final CartAccessService cartAccessService;
    private final CartReadModelService cartReadModelService;
    private final TransactionRunner transactionRunner;

    public UpdateCartLineBillingCycleCommandHandler(CartAccessService cartAccessService,
                                                    CartReadModelService cartReadModelService,
                                                    TransactionRunner transactionRunner) {
        this.cartAccessService = cartAccessService;
        this.cartReadModelService = cartReadModelService;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<CartReadModel> handle(UpdateCartLineBillingCycleCommand command) {
        return transactionRunner.runReturning(() -> {
            Result<Cart> cartResult = cartAccessService.getRequiredActiveCart(command.userId(), command.guestToken());
            if (cartResult.isFailure()) {
                return Result.failure(cartResult.getError());
            }

            Result<Cart> updatedResult = cartResult.getValue()
                    .changeLineBillingCycle(command.lineId(), command.billingCycle());
            if (updatedResult.isFailure()) {
                return Result.failure(updatedResult.getError());
            }

            Cart updated = updatedResult.getValue();
            cartAccessService.save(updated);
            return Result.success(cartReadModelService.toReadModel(updated));
        });
    }
}
