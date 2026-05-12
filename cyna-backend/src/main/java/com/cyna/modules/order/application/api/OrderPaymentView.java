package com.cyna.modules.order.application.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderPaymentView(
        UUID id,
        UUID userId,
        String status,
        BigDecimal totalAmount,
        String currency,
        List<OrderLineView> lines
) {
    public record OrderLineView(
            UUID id,
            UUID productId,
            String productName,
            String productCategory,
            String billingCycle,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
