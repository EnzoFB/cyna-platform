package com.cyna.modules.cart.application.service;

import com.cyna.modules.cart.domain.model.Cart;
import com.cyna.modules.cart.domain.repository.CartRepository;
import com.cyna.shared.domain.Guard;
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

    public Result<Cart> getOrCreateActiveCart(UUID userId, String guestToken) {
        Result<Void> ownerValidation = validateOwner(userId, guestToken);
        if (ownerValidation.isFailure()) {
            return Result.failure(ownerValidation.getError());
        }

        Optional<Cart> existing = userId != null
                ? cartRepository.findActiveByUserId(userId)
                : cartRepository.findActiveByGuestToken(normalizeGuestToken(guestToken));

        if (existing.isPresent()) {
            return Result.success(existing.get());
        }

        Cart created = userId != null
                ? Cart.createForUser(userId)
                : Cart.createForGuest(normalizeGuestToken(guestToken));
        cartRepository.save(created);
        return Result.success(created);
    }

    public Result<Cart> getRequiredActiveCart(UUID userId, String guestToken) {
        Result<Void> ownerValidation = validateOwner(userId, guestToken);
        if (ownerValidation.isFailure()) {
            return Result.failure(ownerValidation.getError());
        }

        Optional<Cart> existing = userId != null
                ? cartRepository.findActiveByUserId(userId)
                : cartRepository.findActiveByGuestToken(normalizeGuestToken(guestToken));

        return existing
                .map(Result::success)
                .orElse(Result.failure("Active cart not found"));
    }

    public void save(Cart cart) {
        cartRepository.save(cart);
    }

    private Result<Void> validateOwner(UUID userId, String guestToken) {
        boolean hasUser = userId != null;
        boolean hasGuest = guestToken != null && !guestToken.isBlank();

        if (hasUser == hasGuest) {
            return Result.failure("Exactly one owner must be provided: userId or guestToken");
        }

        return Result.success();
    }

    private String normalizeGuestToken(String guestToken) {
        Guard.againstNullOrBlank(guestToken, "guestToken");
        return guestToken.trim();
    }
}
