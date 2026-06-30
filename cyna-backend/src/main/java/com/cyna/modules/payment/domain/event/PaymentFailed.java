package com.cyna.modules.payment.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailed(
        UUID paymentId,
        UUID orderId,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {}
