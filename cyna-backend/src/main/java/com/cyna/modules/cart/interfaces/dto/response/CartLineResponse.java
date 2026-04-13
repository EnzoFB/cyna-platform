package com.cyna.modules.cart.interfaces.dto.response;

import com.cyna.modules.cart.application.model.CartLineReadModel;

import java.math.BigDecimal;
import java.util.UUID;

public record CartLineResponse(
        UUID lineId,
        UUID productId,
        String productName,
        String productCategory,
        String billingCycle,
        int quantity,
        BigDecimal unitPrice,
        String currency,
        boolean available
) {
    public static CartLineResponse from(CartLineReadModel model) {
        return new CartLineResponse(
                model.lineId(),
                model.productId(),
                model.productName(),
                model.productCategory(),
                model.billingCycle(),
                model.quantity(),
                model.unitPrice(),
                model.currency(),
                model.available()
        );
    }
}
