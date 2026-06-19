package com.cyna.modules.subscription.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/** Published a few days before a free trial converts to paid. */
public record SubscriptionTrialWillEndIntegrationEvent(
        UUID subscriptionId,
        UUID userId,
        UUID productId,
        Instant trialEndAt,
        Instant occurredAt
) implements IntegrationEvent {}
