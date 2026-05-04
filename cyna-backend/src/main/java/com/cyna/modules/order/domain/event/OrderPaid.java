package com.cyna.modules.order.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderPaid(
        UUID orderId,
        UUID userId,
        BigDecimal totalAmount,
        Instant occurredAt
) implements DomainEvent {}
