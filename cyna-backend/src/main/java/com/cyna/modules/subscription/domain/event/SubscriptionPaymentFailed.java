package com.cyna.modules.subscription.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a renewal payment failed and the subscription transitioned to
 * PAST_DUE. Stripe keeps retrying via its dunning settings; this event lets us
 * proactively warn the customer so they can update their card before the
 * subscription is definitively cancelled.
 */
public record SubscriptionPaymentFailed(
        UUID subscriptionId,
        UUID userId,
        UUID orderId,
        UUID productId,
        Instant occurredAt
) implements DomainEvent {
}
