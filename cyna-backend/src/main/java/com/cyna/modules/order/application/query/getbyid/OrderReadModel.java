package com.cyna.modules.order.application.query.getbyid;

import com.cyna.modules.order.domain.model.BillingAddress;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderReadModel(
        UUID id,
        UUID userId,
        String status,
        BigDecimal subtotalAmount,
        BigDecimal vatAmount,
        BigDecimal totalAmount,
        String currency,
        BillingAddress billingAddress,
        Instant createdAt,
        Instant updatedAt,
        List<OrderLineReadModel> lines
) {
}
