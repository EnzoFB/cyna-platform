package com.cyna.modules.payment.domain.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentGatewayPort {

    SubscriptionResult createSubscription(
            UUID orderId,
            String existingStripeCustomerId,
            String userEmail,
            String userFullName,
            List<SubscriptionLineItem> lineItems,
            String currency,
            String billingCycle
    );

    void cancelSubscription(String stripeSubscriptionId);

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
     * Creates a SetupIntent so the frontend can collect card details via Stripe
     * Elements and attach the resulting PaymentMethod to the customer without
     * charging. Creates the Stripe Customer first if it does not exist yet.
     *
     * @return the SetupIntent client_secret to hand to the frontend
     */
    String createSetupIntent(String stripeCustomerId);

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

    record SubscriptionLineItem(
            UUID productId,
            String productName,
            BigDecimal unitAmount,
            int quantity
    ) {}

    record SubscriptionResult(
            String stripeCustomerId,
            String subscriptionId,
            String scheduleId,
            String clientSecret,
            String paymentIntentId
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
