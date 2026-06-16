package com.cyna.modules.cart.interfaces.dto.response;

import com.cyna.modules.cart.application.model.CheckoutCartReadModel;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutCartResponse(
        UUID cartId,
        UUID orderId,
        BigDecimal subtotalHt,
        String currency
) {
    public static CheckoutCartResponse from(CheckoutCartReadModel model) {
        return new CheckoutCartResponse(
                model.cartId(),
                model.orderId(),
                model.subtotalHt(),
                model.currency()
        );
    }
}
