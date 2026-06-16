package com.cyna.modules.cart.application.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutCartReadModel(
        UUID cartId,
        UUID orderId,
        BigDecimal subtotalHt,
        String currency
) {
}
