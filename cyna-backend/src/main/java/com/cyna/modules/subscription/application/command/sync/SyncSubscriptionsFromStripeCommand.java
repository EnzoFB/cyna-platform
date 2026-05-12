package com.cyna.modules.subscription.application.command.sync;

import com.cyna.shared.application.Command;

import java.time.Instant;

/**
 * Reconciles every local Subscription bound to the given Stripe Subscription with
 * Stripe's authoritative state. Triggered by {@code customer.subscription.updated}
 * (and {@code customer.subscription.created}) webhooks — it's the anti-drift
 * mechanism for all subscription state changes, regardless of whether they
 * originated from our backend, the Stripe dashboard, the customer portal, or
 * Stripe's own lifecycle (period end, dunning…).
 */
public record SyncSubscriptionsFromStripeCommand(
        String stripeSubscriptionId,
        String stripeStatus,
        Boolean cancelAtPeriodEnd,
        Instant currentPeriodEnd,
        Instant stripeCanceledAt
) implements Command<Void> {}
