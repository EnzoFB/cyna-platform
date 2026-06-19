package com.cyna.modules.subscription.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a renewal payment failed (generic decline) and the subscription is PAST_DUE. */
public record SubscriptionPaymentFailedIntegrationEvent(
        UUID subscriptionId,
        UUID userId,
        UUID orderId,
        UUID productId,
        Instant occurredAt
) implements IntegrationEvent {}
