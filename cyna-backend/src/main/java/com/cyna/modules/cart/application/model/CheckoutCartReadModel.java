package com.cyna.modules.cart.application.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutCartReadModel(
        UUID cartId,
        BigDecimal subtotalHt,
        BigDecimal vatAmount,
        BigDecimal totalTtc,
        String currency
) {
}
