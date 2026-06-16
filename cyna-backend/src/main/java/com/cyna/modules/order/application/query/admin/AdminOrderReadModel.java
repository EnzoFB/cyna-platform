package com.cyna.modules.order.application.query.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminOrderReadModel(
    UUID id,
    UUID userId,
    String customerEmail,
    String customerFirstName,
    String customerLastName,
    String status,
    BigDecimal subtotalAmount,
    BigDecimal vatAmount,
    BigDecimal totalAmount,
    String currency,
    long lineCount,
    Instant createdAt,
    Instant updatedAt
) {}
