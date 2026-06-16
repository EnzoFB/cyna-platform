package com.cyna.modules.order.interfaces.rest.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineResponse(
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
