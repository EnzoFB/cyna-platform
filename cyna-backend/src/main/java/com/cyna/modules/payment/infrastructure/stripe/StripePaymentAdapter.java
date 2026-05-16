package com.cyna.modules.payment.infrastructure.stripe;

import com.cyna.modules.payment.config.StripeProperties;
import com.cyna.modules.payment.domain.port.PaymentGatewayException;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort;
import com.cyna.modules.payment.domain.port.WebhookSignatureException;
import com.cyna.modules.payment.domain.repository.StripeProductRepository;
import com.stripe.net.RequestOptions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Component
public class StripePaymentAdapter implements PaymentGatewayPort {

    private final StripeProperties properties;
    private final StripeProductRepository stripeProductRepository;

    public StripePaymentAdapter(StripeProperties properties,
                                StripeProductRepository stripeProductRepository) {
        this.properties = properties;
        this.stripeProductRepository = stripeProductRepository;
        Stripe.apiKey = properties.secretKey();
    }

    @Override
    public SubscriptionForLineResult createSubscriptionForLine(
            String stripeCustomerId,
            String paymentMethodId,
            UUID orderId,
            UUID orderLineId,
            UUID productId,
            String productName,
            BigDecimal unitAmount,
            int quantity,
            String currency,
            String billingCycle) {
        try {
            String stripeProductId = ensureStripeProduct(productId, productName);

            var interval = "ANNUAL".equals(billingCycle)
                    ? SubscriptionCreateParams.Item.PriceData.Recurring.Interval.YEAR
                    : SubscriptionCreateParams.Item.PriceData.Recurring.Interval.MONTH;

            long unitAmountInCents = unitAmount
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();

            SubscriptionCreateParams.Item item = SubscriptionCreateParams.Item.builder()
                    .setPriceData(
                            SubscriptionCreateParams.Item.PriceData.builder()
                                    .setCurrency(currency.toLowerCase())
                                    .setUnitAmount(unitAmountInCents)
                                    .setRecurring(
                                            SubscriptionCreateParams.Item.PriceData.Recurring.builder()
                                                    .setInterval(interval)
                                                    .build()
                                    )
                                    .setProduct(stripeProductId)
                                    .build()
                    )
                    .setQuantity((long) quantity)
                    .build();

            // allow_incomplete: Stripe creates the Subscription, attempts the first
            // invoice off-session using the attached PaymentMethod. If the charge
            // succeeds the Subscription is `active`; if 3DS is required or the card
            // declines, the Subscription is `incomplete` and the customer gets a
            // chance to retry (we surface this via the customer.subscription.updated
            // webhook).
            SubscriptionCreateParams subParams = SubscriptionCreateParams.builder()
                    .setCustomer(stripeCustomerId)
                    .addItem(item)
                    .setDefaultPaymentMethod(paymentMethodId)
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.ALLOW_INCOMPLETE)
                    .putMetadata("cyna_order_id", orderId.toString())
                    .putMetadata("cyna_order_line_id", orderLineId.toString())
                    .build();

            // Idempotency key keyed on order_line_id so a retried finalize call
            // returns the same Stripe Subscription rather than a duplicate.
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("cyna-line-" + orderLineId)
                    .build();

            Subscription sub = Subscription.create(subParams, options);
            return new SubscriptionForLineResult(sub.getId(), sub.getStatus());

        } catch (StripeException e) {
            throw new PaymentGatewayException(
                    "Stripe Subscription creation failed for line " + orderLineId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns the Stripe Product ID for the given Cyna product, creating the
     * Stripe Product on first use and persisting the {cyna→stripe} mapping in
     * {@code payment_schema.stripe_products}. The Cyna product UUID is used as
     * the Stripe Idempotency-Key on creation, so concurrent first-time calls
     * (e.g. two parallel checkouts of the same product just after a backend
     * restart) all receive the same {@code prod_*} ID — no duplicates in Stripe.
     */
    private String ensureStripeProduct(UUID cynaProductId, String productName) throws StripeException {
        var existing = stripeProductRepository.findStripeProductIdByCynaProductId(cynaProductId);
        if (existing.isPresent()) {
            return existing.get();
        }

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("cyna-product-" + cynaProductId)
                .build();
        Product created = Product.create(
                ProductCreateParams.builder()
                        .setName(productName)
                        .putMetadata("cyna_product_id", cynaProductId.toString())
                        .build(),
                options
        );

        stripeProductRepository.save(cynaProductId, created.getId());
        return created.getId();
    }

    @Override
    public String createBillingPortalSession(String stripeCustomerId, String returnUrl) {
        try {
            com.stripe.model.billingportal.Session session =
                    com.stripe.model.billingportal.Session.create(
                            com.stripe.param.billingportal.SessionCreateParams.builder()
                                    .setCustomer(stripeCustomerId)
                                    .setReturnUrl(returnUrl)
                                    .build()
                    );
            return session.getUrl();
        } catch (StripeException e) {
            throw new PaymentGatewayException(
                    "Stripe Customer Portal session creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void setSubscriptionCancelAtPeriodEnd(String stripeSubscriptionId, boolean cancelAtPeriodEnd) {
        try {
            Subscription stripeSub = Subscription.retrieve(stripeSubscriptionId);
            // If Stripe has already terminated the subscription, the flag is meaningless —
            // treat as idempotent no-op so user-initiated cancels remain safe even when
            // a webhook has already finalized the cancellation locally.
            if ("canceled".equals(stripeSub.getStatus())) {
                return;
            }
            stripeSub.update(
                    SubscriptionUpdateParams.builder()
                            .setCancelAtPeriodEnd(cancelAtPeriodEnd)
                            .build()
            );
        } catch (StripeException e) {
            if ("resource_missing".equals(e.getCode())) {
                return;
            }
            throw new PaymentGatewayException(
                    "Stripe Subscription cancel_at_period_end update failed: " + e.getMessage(), e);
        }
    }

    @Override
    public StripeWebhookEvent parseWebhookEvent(String payload, String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, properties.webhookSecret());

            // We bypass the SDK's typed deserialization because Stripe's account API
            // version (e.g. 2025-03-31.basil) often differs from the version pinned
            // in the SDK (2024-06-20 in stripe-java 26.x). When schemas diverge,
            // typed deserialization silently returns Optional.empty() and we lose
            // every field. Parsing the raw JSON keeps the handler version-agnostic.
            String rawJson = event.getDataObjectDeserializer().getRawJson();
            JsonObject obj = JsonParser.parseString(rawJson).getAsJsonObject();

            String type = event.getType();
            String paymentIntentId = null;
            String subscriptionId = null;
            String customerId = jsonString(obj, "customer");
            String invoiceId = null;
            String billingReason = null;
            Instant periodEnd = null;
            String subscriptionStatus = null;
            Boolean cancelAtPeriodEnd = null;
            Instant currentPeriodEnd = null;
            Instant canceledAt = null;
            String paymentMethodId = null;

            if (type.startsWith("payment_intent.")) {
                // PaymentIntent payload: id, customer, latest_invoice, ...
                paymentIntentId = jsonString(obj, "id");
            } else if (type.startsWith("invoice.")) {
                invoiceId = jsonString(obj, "id");
                billingReason = jsonString(obj, "billing_reason");
                // 2024 schema: invoice.subscription is a top-level string
                // 2025-03-31.basil schema: moved under invoice.parent.subscription
                subscriptionId = jsonString(obj, "subscription");
                if (subscriptionId == null && obj.has("parent") && obj.get("parent").isJsonObject()) {
                    subscriptionId = jsonString(obj.getAsJsonObject("parent"), "subscription");
                }
                // payment_intent on invoice was removed in 2025-08-27. We rely on the
                // payment_intent.succeeded event for the PI id of the first invoice.
                paymentIntentId = jsonString(obj, "payment_intent");
                Long pe = jsonLong(obj, "period_end");
                if (pe != null) {
                    periodEnd = Instant.ofEpochSecond(pe);
                }
            } else if (type.startsWith("customer.subscription.")) {
                // The Subscription object IS the data object here. We extract everything
                // needed to reconcile our local mirror with Stripe's authoritative state.
                subscriptionId = jsonString(obj, "id");
                subscriptionStatus = jsonString(obj, "status");
                cancelAtPeriodEnd = jsonBoolean(obj, "cancel_at_period_end");
                // current_period_end moved under items.data[0].current_period_end in
                // the 2025-04-30 API version — fall back to the legacy top-level field.
                Long cpe = jsonLong(obj, "current_period_end");
                if (cpe == null && obj.has("items") && obj.get("items").isJsonObject()) {
                    JsonObject items = obj.getAsJsonObject("items");
                    if (items.has("data") && items.get("data").isJsonArray()
                            && items.getAsJsonArray("data").size() > 0) {
                        JsonObject firstItem = items.getAsJsonArray("data").get(0).getAsJsonObject();
                        cpe = jsonLong(firstItem, "current_period_end");
                    }
                }
                if (cpe != null) {
                    currentPeriodEnd = Instant.ofEpochSecond(cpe);
                }
                Long ca = jsonLong(obj, "canceled_at");
                if (ca != null) {
                    canceledAt = Instant.ofEpochSecond(ca);
                }
            } else if (type.startsWith("payment_method.")) {
                // The PaymentMethod object IS the data object here. We still
                // parse the id (the event is acked, not routed — Stripe is the
                // single source of truth for cards; we keep no local mirror).
                paymentMethodId = jsonString(obj, "id");
            }

            return new StripeWebhookEvent(
                    event.getId(),
                    type, paymentIntentId, subscriptionId, customerId,
                    invoiceId, billingReason, periodEnd,
                    subscriptionStatus, cancelAtPeriodEnd, currentPeriodEnd, canceledAt,
                    paymentMethodId
            );

        } catch (SignatureVerificationException e) {
            throw new WebhookSignatureException("Invalid Stripe webhook signature", e);
        }
    }

    private static String jsonString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    private static Long jsonLong(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsLong();
    }

    private static Boolean jsonBoolean(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsBoolean();
    }

    @Override
    public String createCustomerForUser(String email, String fullName) {
        try {
            var params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(fullName)
                    .build();
            return Customer.create(params).getId();
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to create Stripe customer: " + e.getMessage(), e);
        }
    }

    @Override
    public SetupIntentResult createSetupIntent(String stripeCustomerId) {
        try {
            var params = SetupIntentCreateParams.builder()
                    .setCustomer(stripeCustomerId)
                    .addPaymentMethodType("card")
                    // usage=off_session so the resulting PaymentMethod can be charged
                    // later (subscription renewals) without re-collecting from the user.
                    .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION)
                    .build();
            SetupIntent intent = SetupIntent.create(params);
            return new SetupIntentResult(intent.getId(), intent.getClientSecret());
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to create SetupIntent: " + e.getMessage(), e);
        }
    }

    @Override
    public SavedPaymentMethodDetails attachPaymentMethod(String stripeCustomerId, String paymentMethodId) {
        try {
            var pm = PaymentMethod.retrieve(paymentMethodId);
            pm.attach(PaymentMethodAttachParams.builder().setCustomer(stripeCustomerId).build());

            var card = pm.getCard();
            String brand = card != null ? capitalize(card.getBrand()) : "Card";
            String last4 = card != null ? card.getLast4() : "????";
            String expMonth = card != null ? String.format("%02d", card.getExpMonth()) : "??";
            String expYear = card != null ? String.valueOf(card.getExpYear()) : "????";
            String holderName = pm.getBillingDetails() != null ? pm.getBillingDetails().getName() : null;

            return new SavedPaymentMethodDetails(brand, last4, expMonth, expYear, holderName);
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to attach PaymentMethod: " + e.getMessage(), e);
        }
    }


    @Override
    public java.util.List<InvoiceSummary> listInvoices(String stripeCustomerId) {
        try {
            com.stripe.param.InvoiceListParams params = com.stripe.param.InvoiceListParams.builder()
                    .setCustomer(stripeCustomerId)
                    .setLimit(100L)
                    .build();

            java.util.List<InvoiceSummary> out = new java.util.ArrayList<>();
            for (com.stripe.model.Invoice inv : com.stripe.model.Invoice.list(params).autoPagingIterable()) {
                // Skip draft/void invoices — only surface what the customer can
                // actually act on or keep for accounting (paid / open).
                String status = inv.getStatus();
                if (status == null || "draft".equals(status) || "void".equals(status)) {
                    continue;
                }
                BigDecimal amountPaid = inv.getAmountPaid() != null
                        ? new BigDecimal(inv.getAmountPaid()).movePointLeft(2)
                        : BigDecimal.ZERO;
                Instant createdAt = inv.getCreated() != null
                        ? Instant.ofEpochSecond(inv.getCreated())
                        : Instant.now();
                out.add(new InvoiceSummary(
                        inv.getId(),
                        inv.getNumber(),
                        status,
                        amountPaid,
                        inv.getCurrency() != null ? inv.getCurrency().toUpperCase() : "EUR",
                        createdAt,
                        inv.getHostedInvoiceUrl(),
                        inv.getInvoicePdf()
                ));
            }
            return out;
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to list invoices: " + e.getMessage(), e);
        }
    }

    @Override
    public java.util.List<PaymentMethodSummary> listPaymentMethods(String stripeCustomerId) {
        try {
            // The card considered "default" is the one Stripe will charge for any
            // invoice/subscription that doesn't pin its own PM.
            String defaultPmId = null;
            Customer customer = Customer.retrieve(stripeCustomerId);
            if (customer.getInvoiceSettings() != null) {
                defaultPmId = customer.getInvoiceSettings().getDefaultPaymentMethod();
            }

            com.stripe.param.PaymentMethodListParams params =
                    com.stripe.param.PaymentMethodListParams.builder()
                            .setCustomer(stripeCustomerId)
                            .setType(com.stripe.param.PaymentMethodListParams.Type.CARD)
                            .setLimit(100L)
                            .build();

            java.util.List<PaymentMethodSummary> out = new java.util.ArrayList<>();
            for (PaymentMethod pm : PaymentMethod.list(params).autoPagingIterable()) {
                var card = pm.getCard();
                String brand = card != null ? capitalize(card.getBrand()) : "Card";
                String last4 = card != null ? card.getLast4() : "????";
                String expMonth = card != null ? String.format("%02d", card.getExpMonth()) : "??";
                String expYear = card != null ? String.valueOf(card.getExpYear()) : "????";
                String holderName = pm.getBillingDetails() != null
                        ? pm.getBillingDetails().getName() : null;
                out.add(new PaymentMethodSummary(
                        pm.getId(), brand, last4, expMonth, expYear, holderName,
                        pm.getId().equals(defaultPmId)));
            }
            // Default first, then Stripe's natural (most-recent) order.
            out.sort((a, b) -> Boolean.compare(b.isDefault(), a.isDefault()));
            return out;
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to list payment methods: " + e.getMessage(), e);
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
