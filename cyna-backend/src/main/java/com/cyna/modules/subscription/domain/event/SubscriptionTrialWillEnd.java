package com.cyna.modules.subscription.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised a few days before a free trial converts to a paid subscription,
 * triggered by Stripe's {@code customer.subscription.trial_will_end} webhook
 * (Stripe fires it ~3 days before {@code trial_end}). The notification module
 * turns it into a "your trial is ending" email so the customer is warned before
 * the first charge — important for annual plans, where the first invoice is the
 * full year.
 *
 * <p>Carries only ids plus {@code trialEndAt}; the recipient and product label
 * are resolved by the notification module through the published cross-module
 * query seams, like the other subscription lifecycle events.
 */
public record SubscriptionTrialWillEnd(
        UUID subscriptionId,
        UUID userId,
        UUID productId,
        Instant trialEndAt,
        Instant occurredAt
) implements DomainEvent {}
