package com.cyna.modules.payment.application.api;

import com.cyna.shared.domain.Result;

public interface PaymentCommandApi {

    /**
     * Toggles {@code cancel_at_period_end} on the Stripe Subscription. This is the
     * primitive behind both user-initiated cancellation (true) and auto-renew
     * re-activation (false). The customer keeps access until the end of the current
     * period; no further charges are made if {@code cancelAtPeriodEnd=true}.
     *
     * <p>Idempotent. Returns {@code Result.success()} for an empty/missing Stripe id.
     */
    Result<Void> setStripeSubscriptionCancelAtPeriodEnd(String stripeSubscriptionId, boolean cancelAtPeriodEnd);
}
