package com.cyna.modules.subscription.application.api;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.shared.domain.Result;

import java.time.Instant;

public interface SubscriptionCommandApi {

    Result<SubscriptionReadModel> createFromPayment(SubscriptionPaymentPayload payload);

    Result<Void> renewByStripeId(String stripeSubscriptionId, Instant newPeriodEnd);

    Result<Void> markPastDueByStripeId(String stripeSubscriptionId);

    Result<Void> cancelByStripeId(String stripeSubscriptionId);

    /**
     * Reconciles every local subscription bound to {@code stripeSubscriptionId} with
     * Stripe's authoritative state. Called from the payment webhook on
     * {@code customer.subscription.updated} / {@code .created}. This is the
     * anti-drift mechanism — it catches changes made outside our backend (Stripe
     * dashboard, customer portal, Stripe lifecycle), and is idempotent enough to
     * also absorb the post-write echo of changes we initiated ourselves.
     */
    Result<Void> syncFromStripeState(String stripeSubscriptionId,
                                     String stripeStatus,
                                     Boolean cancelAtPeriodEnd,
                                     Instant currentPeriodEnd,
                                     Instant stripeCanceledAt);
}
