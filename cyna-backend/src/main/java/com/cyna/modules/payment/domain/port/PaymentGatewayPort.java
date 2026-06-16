package com.cyna.modules.payment.domain.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
            UUID userId,
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
     * Cancels a Stripe Subscription <em>immediately</em> (not at period end). Used to
     * roll back Subscriptions created during a checkout whose first off-session
     * charge was declined, so no half-provisioned subscription is left behind at
     * Stripe. {@code incomplete} subscriptions were never charged, so this is a
     * clean rollback with no money to refund.
     *
     * <p>Idempotent: a no-op if the subscription is already canceled or no longer
     * exists at Stripe ({@code resource_missing}).
     */
    void cancelSubscriptionNow(String stripeSubscriptionId);

    /**
     * Pins, on the Stripe Customer, everything Stripe Tax needs to compute the
     * correct VAT on subscription invoices, BEFORE any subscription is created:
     *
     * <ol>
     *   <li><b>Billing address</b> — copied from {@code paymentMethodId}'s
     *       billing details onto {@code customer.address}. Stripe Tax derives
     *       the destination VAT jurisdiction from this address.</li>
     *   <li><b>VAT number (B2B)</b> — when {@code vatNumber} is non-blank it is
     *       attached as a Stripe {@code tax_id} (type inferred from the VAT
     *       prefix: {@code eu_vat} for EU member states, {@code gb_vat},
     *       {@code ch_vat}, {@code no_vat}). This is what makes Stripe apply the
     *       <b>intra-EU reverse charge</b> (0% VAT, "autoliquidation") for a
     *       valid cross-border B2B number instead of charging VAT as B2C.</li>
     * </ol>
     *
     * <p>No-op when Stripe Tax is disabled. The <b>address</b> step fails closed
     * (throws) if Stripe rejects it, so we never charge an incorrectly-taxed
     * amount; a PaymentMethod with no usable address is skipped (Stripe then
     * fails closed at subscription creation). The <b>VAT number</b> step is
     * best-effort: a malformed/unrecognized number is logged and skipped rather
     * than aborting an otherwise-valid payment — falling back to charging VAT
     * (B2C) is the safe outcome (never under-charge). Idempotent: re-applying
     * the same address or an already-attached VAT id is harmless.
     */
    void updateCustomerTaxLocation(String stripeCustomerId, String paymentMethodId, String vatNumber);

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

    /**
     * Creates a new Stripe Customer and returns its id. The Cyna user id is
     * stamped on the Customer's metadata so the link survives a DB loss: any
     * Stripe Customer can be traced back to its Cyna user from the dashboard.
     */
    String createCustomerForUser(UUID userId, String email, String fullName);

    /**
     * Lists every card PaymentMethod attached to the customer. Stripe is the
     * single source of truth — the UI reads this directly so it never diverges
     * from what Stripe actually holds (no dependency on webhook timing).
     * {@code isDefault} reflects {@code Customer.invoice_settings
     * .default_payment_method}. Empty list if the customer has none.
     */
    List<PaymentMethodSummary> listPaymentMethods(String stripeCustomerId);

    record PaymentMethodSummary(
            String stripePaymentMethodId,
            String brand,
            String last4,
            String expMonth,
            String expYear,
            String holderName,
            boolean isDefault
    ) {}

    /**
     * Lists the customer's invoices, most recent first. Stripe is the system of
     * record for invoices (legal retention, PDF generation, numbering); we only
     * surface links. Returns an empty list if the customer has none.
     */
    List<InvoiceSummary> listInvoices(String stripeCustomerId);

    /**
     * One Stripe invoice. {@code hostedInvoiceUrl} is the Stripe-hosted page
     * (viewable, printable); {@code invoicePdfUrl} is the direct PDF download.
     * Both are signed Stripe URLs — never stored, always fetched fresh.
     */
    record InvoiceSummary(
            String id,
            String number,
            String status,
            BigDecimal amountPaid,
            String currency,
            Instant createdAt,
            String hostedInvoiceUrl,
            String invoicePdfUrl
    ) {}

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
            String status,
            // current_period_end as Stripe set it on the subscription (epoch-aware,
            // accounts for prorations and Stripe's billing clock). Null on API
            // versions/states where Stripe doesn't surface it — caller falls back
            // to a local +1 month/year estimate and the webhook reconciles later.
            Instant currentPeriodEnd,
            // Status of latest_invoice.payment_intent (PSD2 / 3DS):
            //  - "succeeded"        → money moved; sub will be active.
            //  - "requires_action"  → SCA challenge required; client must use
            //    paymentIntentClientSecret with stripe.confirmCardPayment to
            //    complete 3DS. This is the case our old code wrongly handled
            //    as a decline.
            //  - "requires_payment_method" / "canceled" → genuine decline,
            //    caller rolls back.
            //  - null               → no PaymentIntent on this invoice (free
            //    trial, $0 first invoice, or older API version not exposing it).
            String paymentIntentStatus,
            String paymentIntentClientSecret
    ) {}

    record StripeWebhookEvent(
            // Stripe event id (evt_...). Stable across redeliveries — the
            // idempotency key used to deduplicate at-least-once webhook
            // delivery. Null only for manually-built events in unit tests.
            String eventId,
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
            Instant canceledAt,
            // Populated on `payment_method.*` events. Null otherwise.
            String paymentMethodId,
            // Populated on `invoice.payment_action_required` (and any other
            // invoice.* event where Stripe surfaces it). Stripe-signed URL the
            // customer can be sent to in order to complete a renewal SCA
            // challenge — embedded in the dunning email so the user can
            // recover without leaving Stripe-hosted UI. Null otherwise.
            String hostedInvoiceUrl
    ) {
        /**
         * Compat constructor for callers that pre-date {@code eventId} /
         * {@code paymentMethodId} (unit tests building events by hand — they
         * don't exercise the dedup path).
         */
        public StripeWebhookEvent(
                String type,
                String paymentIntentId,
                String subscriptionId,
                String customerId,
                String invoiceId,
                String billingReason,
                Instant periodEnd,
                String subscriptionStatus,
                Boolean cancelAtPeriodEnd,
                Instant currentPeriodEnd,
                Instant canceledAt) {
            this(null, type, paymentIntentId, subscriptionId, customerId, invoiceId,
                    billingReason, periodEnd, subscriptionStatus, cancelAtPeriodEnd,
                    currentPeriodEnd, canceledAt, null, null);
        }
    }
}
