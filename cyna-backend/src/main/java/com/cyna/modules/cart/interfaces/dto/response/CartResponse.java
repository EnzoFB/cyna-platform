package com.cyna.modules.cart.interfaces.dto.response;

import com.cyna.modules.cart.application.model.CartReadModel;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        UUID userId,
        String status,
        List<CartLineResponse> lines,
        CartTotalsResponse totals,
        boolean checkoutAllowed,
        List<String> errors
) {
    public static CartResponse from(CartReadModel model) {
        return new CartResponse(
                model.cartId(),
                model.userId(),
                model.status(),
                model.lines().stream().map(CartLineResponse::from).toList(),
                CartTotalsResponse.from(model.totals()),
                model.checkoutAllowed(),
                model.errors()
        );
    }
}
