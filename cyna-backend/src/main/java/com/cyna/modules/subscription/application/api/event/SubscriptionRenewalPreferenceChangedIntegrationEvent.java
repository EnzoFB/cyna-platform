package com.cyna.modules.subscription.application.api.event;

import com.cyna.shared.application.IntegrationEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when the user changes a subscription's renewal preference. The
 * payment module reacts by syncing {@code cancel_at_period_end = !autoRenew}
 * on the Stripe Subscription.
 */
public record SubscriptionRenewalPreferenceChangedIntegrationEvent(
        UUID subscriptionId,
        String stripeSubscriptionId,
        boolean autoRenew,
        Instant occurredAt
) implements IntegrationEvent {}
