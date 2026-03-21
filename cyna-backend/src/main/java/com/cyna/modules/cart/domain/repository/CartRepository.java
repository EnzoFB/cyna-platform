package com.cyna.modules.cart.domain.repository;

import com.cyna.modules.cart.domain.model.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository {

    void save(Cart cart);

    Optional<Cart> findById(UUID cartId);

    Optional<Cart> findActiveByUserId(UUID userId);

    Optional<Cart> findActiveByGuestToken(String guestToken);

    void deleteById(UUID cartId);
}
