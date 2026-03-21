package com.cyna.modules.cart.application.command.addline;

import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.modules.cart.application.service.CartAccessService;
import com.cyna.modules.cart.application.service.CartReadModelService;
import com.cyna.modules.cart.domain.model.Cart;
import com.cyna.modules.product.application.api.ProductInfo;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class AddCartLineCommandHandler implements CommandHandler<AddCartLineCommand, CartReadModel> {

    private final CartAccessService cartAccessService;
    private final CartReadModelService cartReadModelService;
    private final TransactionRunner transactionRunner;

    public AddCartLineCommandHandler(CartAccessService cartAccessService,
                                     CartReadModelService cartReadModelService,
                                     TransactionRunner transactionRunner) {
        this.cartAccessService = cartAccessService;
        this.cartReadModelService = cartReadModelService;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<CartReadModel> handle(AddCartLineCommand command) {
        return transactionRunner.runReturning(() -> {
            Result<Cart> cartResult = cartAccessService.getOrCreateActiveCart(command.userId(), command.guestToken());
            if (cartResult.isFailure()) {
                return Result.failure(cartResult.getError());
            }

            Cart cart = cartResult.getValue();
            Result<ProductInfo> productResult = cartReadModelService.getPurchasableProduct(command.productId());
            if (productResult.isFailure()) {
                return Result.failure(productResult.getError());
            }

            var product = productResult.getValue();
            Result<Cart> updatedResult = cart.addOrMergeLine(
                    product.id(),
                    product.name(),
                    product.category(),
                    command.billingCycle(),
                    command.quantity()
            );
            if (updatedResult.isFailure()) {
                return Result.failure(updatedResult.getError());
            }

            Cart updated = updatedResult.getValue();
            cartAccessService.save(updated);
            return Result.success(cartReadModelService.toReadModel(updated));
        });
    }
}
