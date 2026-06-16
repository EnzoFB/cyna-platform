package com.cyna.modules.cart.interfaces.dto.response;

import com.cyna.modules.cart.application.model.CartTotalsReadModel;

import java.math.BigDecimal;

public record CartTotalsResponse(
        BigDecimal subtotalHt,
        String currency
) {
    public static CartTotalsResponse from(CartTotalsReadModel model) {
        if (model == null) {
            return null;
        }
        return new CartTotalsResponse(
                model.subtotalHt(),
                model.currency()
        );
    }
}
