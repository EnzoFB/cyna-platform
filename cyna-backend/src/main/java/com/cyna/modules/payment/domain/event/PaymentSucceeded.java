package com.cyna.modules.payment.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSucceeded(
        UUID paymentId,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String stripeSubscriptionId,
        Instant occurredAt
) implements DomainEvent {}
