package com.cyna.modules.order.application.query.getbyid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderReadModel(
        UUID id,
        UUID userId,
        String status,
        BigDecimal subtotalHt,
        String currency,
        BillingAddressView billingAddress,
        Instant createdAt,
        Instant updatedAt,
        List<OrderLineReadModel> lines
) {
}
