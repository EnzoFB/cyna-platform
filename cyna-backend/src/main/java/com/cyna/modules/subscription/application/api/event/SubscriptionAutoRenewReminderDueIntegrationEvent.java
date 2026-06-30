package com.cyna.modules.subscription.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/** Published by the auto-renew reminder batch; self-contained (recipient already resolved). */
public record SubscriptionAutoRenewReminderDueIntegrationEvent(
        UUID subscriptionId,
        UUID userId,
        String email,
        String firstName,
        String productName,
        String renewalDate,
        String lang,
        Instant occurredAt
) implements IntegrationEvent {}
