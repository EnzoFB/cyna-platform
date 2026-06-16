package com.cyna.modules.payment.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The Payment associated with an Order succeeded. Consumers that need the
 * individual Stripe Subscription ids fan out via the Subscription module's
 * {@code SubscriptionQueryApi} (one Stripe Subscription per OrderLine in V14+).
 * Carrying a single {@code stripeSubscriptionId} on this event would have been
 * misleading — kept off the contract on purpose.
 */
public record PaymentSucceeded(
        UUID paymentId,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        Instant occurredAt
) implements DomainEvent {}
