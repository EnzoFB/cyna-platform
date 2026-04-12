package com.cyna.modules.cart.application.service;

import com.cyna.modules.cart.domain.model.Cart;
import com.cyna.modules.cart.domain.repository.CartRepository;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CartAccessService {

    private final CartRepository cartRepository;

    public CartAccessService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public Result<Cart> getOrCreateActiveCart(UUID userId) {
        Result<Void> ownerValidation = validateOwner(userId);
        if (ownerValidation.isFailure()) {
            return Result.failure(ownerValidation.getError());
        }

        Optional<Cart> existing = cartRepository.findActiveByUserId(userId);
        if (existing.isPresent()) {
            return Result.success(existing.get());
        }

        Cart created = Cart.createForUser(userId);
        cartRepository.save(created);
        return Result.success(created);
    }

    public Result<Cart> getRequiredActiveCart(UUID userId) {
        Result<Void> ownerValidation = validateOwner(userId);
        if (ownerValidation.isFailure()) {
            return Result.failure(ownerValidation.getError());
        }

        return cartRepository.findActiveByUserId(userId)
                .map(Result::success)
                .orElse(Result.failure("Active cart not found"));
    }

    public void save(Cart cart) {
        cartRepository.save(cart);
    }

    private Result<Void> validateOwner(UUID userId) {
        if (userId == null) {
            return Result.failure("Authenticated user is required");
        }
        return Result.success();
    }
}
