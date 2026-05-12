package com.cyna.modules.payment.domain.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PaymentGatewayPort {

    /**
     * Creates a SetupIntent so the frontend can collect a card via Stripe Elements
     * and attach the resulting PaymentMethod to the customer for future off-session
     * charges (subscriptions, saved cards). Returns the SetupIntent id and its
     * client_secret. The id is what the backend keeps; the client_secret is what
     * the frontend uses to confirm.
     *
     * <p>Used by both the V14 checkout flow (multi-product mixed-cycle) and the
     * "save a card outside checkout" flow.
     */
    SetupIntentResult createSetupIntent(String stripeCustomerId);

    /**
     * Creates ONE Stripe Subscription corresponding to ONE OrderLine and charges
     * its first invoice immediately using the provided PaymentMethod. Used during
     * the multi-product checkout finalize step — called N times (one per line)
     * with the same {@code paymentMethodId} so every line gets its own Stripe
     * Subscription with its own cycle.
     *
     * <p>The Stripe Subscription carries {@code cyna_order_id} and
     * {@code cyna_order_line_id} metadata so downstream webhooks can route each
     * event to the correct local Subscription.
     *
     * <p>Idempotency: the Stripe call uses
     * {@code idempotency-key = "cyna-line-" + orderLineId} so retries are safe.
     */
    SubscriptionForLineResult createSubscriptionForLine(
            String stripeCustomerId,
            String paymentMethodId,
            UUID orderId,
            UUID orderLineId,
            UUID productId,
            String productName,
            BigDecimal unitAmount,
            int quantity,
            String currency,
            String billingCycle
    );

    /**
     * Toggles the Stripe Subscription's {@code cancel_at_period_end} flag. This is the
     * primitive behind both "cancel subscription" and "disable auto-renew" — the customer
     * keeps service until {@code current_period_end} and is not charged for the next cycle.
     * Passing {@code false} reverses a previously scheduled cancellation.
     *
     * <p>Idempotent: re-applying the same flag is a no-op. Calling on a subscription
     * already terminated at Stripe is treated as a no-op (not an error).
     */
    void setSubscriptionCancelAtPeriodEnd(String stripeSubscriptionId, boolean cancelAtPeriodEnd);

    /**
     * Creates a Stripe Customer Portal session for the given customer. Returns the
     * one-time signed URL the user can be redirected to in order to manage their
     * payment methods, view invoices and cancel subscriptions on Stripe-hosted UI.
     */
    String createBillingPortalSession(String stripeCustomerId, String returnUrl);

    StripeWebhookEvent parseWebhookEvent(String payload, String sigHeader);

    /**
     * Attaches an existing PaymentMethod (pm_xxx) to the given Stripe Customer
     * and returns its display metadata (brand, last4, expiry…).
     */
    SavedPaymentMethodDetails attachPaymentMethod(String stripeCustomerId, String paymentMethodId);

    /** Detaches a PaymentMethod from its customer so it can no longer be charged. */
    void detachPaymentMethod(String stripePaymentMethodId);

    /** Creates a new Stripe Customer and returns its id. */
    String createCustomerForUser(String email, String fullName);

    record SavedPaymentMethodDetails(
            String brand,
            String last4,
            String expMonth,
            String expYear,
            String holderName
    ) {}

    record SetupIntentResult(
            String setupIntentId,
            String clientSecret
    ) {}

    record SubscriptionForLineResult(
            String stripeSubscriptionId,
            // Status from Stripe right after creation: "active", "incomplete", "trialing"…
            // Caller uses this to decide whether to immediately reflect ACTIVE locally
            // or wait for the customer.subscription.updated webhook to confirm.
            String status
    ) {}

    record StripeWebhookEvent(
            String type,
            String paymentIntentId,
            String subscriptionId,
            String customerId,
            String invoiceId,
            String billingReason,
            Instant periodEnd,
            // Populated on `customer.subscription.*` events — used to reconcile local state
            // with Stripe's authoritative view (status, cancel_at_period_end, period end…).
            // Null for non-subscription events.
            String subscriptionStatus,
            Boolean cancelAtPeriodEnd,
            Instant currentPeriodEnd,
            Instant canceledAt
    ) {}
}
