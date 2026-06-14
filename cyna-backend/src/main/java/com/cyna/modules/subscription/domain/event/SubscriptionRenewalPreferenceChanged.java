package com.cyna.modules.subscription.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * The user changed whether a subscription should renew at the end of its period
 * (explicit cancellation, or an auto-renew toggle). Stripe is the authoritative
 * side for the renewal flag, so the payment module reacts to this event by
 * syncing {@code cancel_at_period_end} on the Stripe Subscription.
 *
 * <p>Published instead of calling the payment module directly, so the
 * subscription module stays free of any dependency on payment — the
 * subscription→payment edge that would otherwise close a module cycle.
 *
 * @param autoRenew the new renewal preference; the payment side maps it to
 *                  Stripe's {@code cancel_at_period_end = !autoRenew}.
 */
public record SubscriptionRenewalPreferenceChanged(
        UUID subscriptionId,
        String stripeSubscriptionId,
        boolean autoRenew,
        Instant occurredAt
) implements DomainEvent {
}
