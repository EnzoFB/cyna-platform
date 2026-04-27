package com.cyna.modules.order.application.query.getbyid;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineReadModel(
        UUID id,
        UUID productId,
        String productName,
        String productCategory,
        String billingCycle,
        int quantity,
        BigDecimal unitPrice,
        String currency
) {
}
