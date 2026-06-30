package com.cyna.modules.cart.application.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CartLineReadModel(
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
}
