package com.cyna.modules.cart.application.command.mergeguest;

import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.modules.cart.application.service.CartAccessService;
import com.cyna.modules.cart.application.service.CartReadModelService;
import com.cyna.modules.cart.domain.model.Cart;
import com.cyna.modules.cart.domain.repository.CartRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class MergeGuestCartCommandHandler implements CommandHandler<MergeGuestCartCommand, CartReadModel> {

    private final CartAccessService cartAccessService;
    private final CartReadModelService cartReadModelService;
    private final CartRepository cartRepository;
    private final TransactionRunner transactionRunner;

    public MergeGuestCartCommandHandler(CartAccessService cartAccessService,
                                        CartReadModelService cartReadModelService,
                                        CartRepository cartRepository,
                                        TransactionRunner transactionRunner) {
        this.cartAccessService = cartAccessService;
        this.cartReadModelService = cartReadModelService;
        this.cartRepository = cartRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<CartReadModel> handle(MergeGuestCartCommand command) {
        return transactionRunner.runReturning(() -> {
            if (command.userId() == null) {
                return Result.failure("Authenticated user is required");
            }
            if (command.guestToken() == null || command.guestToken().isBlank()) {
                return Result.failure("Guest token is required");
            }

            Result<Cart> userCartResult = cartAccessService.getOrCreateActiveCart(command.userId(), null);
            if (userCartResult.isFailure()) {
                return Result.failure(userCartResult.getError());
            }

            var guestCartOpt = cartRepository.findActiveByGuestToken(command.guestToken().trim());
            if (guestCartOpt.isEmpty()) {
                return Result.success(cartReadModelService.toReadModel(userCartResult.getValue()));
            }

            Cart userCart = userCartResult.getValue();
            Cart guestCart = guestCartOpt.get();

            if (userCart.getId().equals(guestCart.getId())) {
                return Result.success(cartReadModelService.toReadModel(userCart));
            }

            Result<Cart> mergedResult = userCart.mergeFrom(guestCart);
            if (mergedResult.isFailure()) {
                return Result.failure(mergedResult.getError());
            }

            cartRepository.deleteById(guestCart.getId());
            Cart merged = mergedResult.getValue();
            cartAccessService.save(merged);

            return Result.success(cartReadModelService.toReadModel(merged));
        });
    }
}
