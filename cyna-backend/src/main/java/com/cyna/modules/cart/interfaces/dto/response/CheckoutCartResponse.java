package com.cyna.modules.cart.interfaces.dto.response;

import com.cyna.modules.cart.application.model.CheckoutCartReadModel;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutCartResponse(
        UUID cartId,
        BigDecimal subtotalHt,
        BigDecimal vatAmount,
        BigDecimal totalTtc,
        String currency
) {
    public static CheckoutCartResponse from(CheckoutCartReadModel model) {
        return new CheckoutCartResponse(
                model.cartId(),
                model.subtotalHt(),
                model.vatAmount(),
                model.totalTtc(),
                model.currency()
        );
    }
}
