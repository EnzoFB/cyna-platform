package com.cyna.modules.subscription.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/** Published when a subscription is cancelled. */
public record SubscriptionCancelledIntegrationEvent(
        UUID subscriptionId,
        UUID userId,
        UUID orderId,
        UUID productId,
        Instant occurredAt
) implements IntegrationEvent {}
