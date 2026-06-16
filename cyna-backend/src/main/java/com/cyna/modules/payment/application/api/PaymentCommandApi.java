package com.cyna.modules.payment.application.api;

import com.cyna.shared.domain.Result;

import java.util.UUID;

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

    /**
     * RGPD erasure hook called when a user account is anonymized. Drops the
     * <em>local</em> card-metadata cache ({@code saved_payment_methods}) for
     * the user — data minimization on our side.
     *
     * <p>Deliberately does NOT touch the Stripe Customer or the
     * {@code payment_consent_log}: the Stripe Customer is retained by Stripe
     * (our processor under a DPA) for the legal invoice-retention period, and
     * the consent log is the Art. 7.1 proof that must outlive the card. PAN
     * never reaches our systems (SAQ A), so nothing sensitive remains locally.
     */
    Result<Void> purgeLocalPaymentDataForUser(UUID userId);
}
