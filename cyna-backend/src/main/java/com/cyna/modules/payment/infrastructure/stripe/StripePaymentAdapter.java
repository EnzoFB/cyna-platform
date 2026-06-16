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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Component
public class StripePaymentAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentAdapter.class);

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
            UUID userId,
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

            SubscriptionCreateParams.Item.PriceData.Builder priceData =
                    SubscriptionCreateParams.Item.PriceData.builder()
                            .setCurrency(currency.toLowerCase())
                            .setUnitAmount(unitAmountInCents)
                            .setRecurring(
                                    SubscriptionCreateParams.Item.PriceData.Recurring.builder()
                                            .setInterval(interval)
                                            .build()
                            )
                            .setProduct(stripeProductId);
            // Catalog prices are tax-EXCLUSIVE (HT): Stripe Tax adds the right
            // VAT on top per the customer's billing jurisdiction. Without this
            // the price is treated as `unspecified` and no tax is applied.
            if (properties.taxEnabled()) {
                priceData.setTaxBehavior(
                        SubscriptionCreateParams.Item.PriceData.TaxBehavior.EXCLUSIVE);
            }

            SubscriptionCreateParams.Item item = SubscriptionCreateParams.Item.builder()
                    .setPriceData(priceData.build())
                    .setQuantity((long) quantity)
                    .build();

            // allow_incomplete + expand(latest_invoice.payment_intent): Stripe
            // creates the Subscription and attempts the first off-session charge.
            // - `active`/`trialing`  → settled, money moved.
            // - `incomplete` AND latest_invoice.payment_intent.status =
            //   `requires_action` → SCA challenge (PSD2). The PaymentIntent's
            //   client_secret is propagated to the caller so the frontend can
            //   trigger `stripe.confirmCardPayment` and complete 3DS — without
            //   this expand the secret never surfaces and we wrongly treat the
            //   SCA case as a decline.
            // - `incomplete` with any other PI status → genuine decline, caller
            //   rolls back.
            SubscriptionCreateParams.Builder subBuilder = SubscriptionCreateParams.builder()
                    .setCustomer(stripeCustomerId)
                    .addItem(item)
                    .setDefaultPaymentMethod(paymentMethodId)
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.ALLOW_INCOMPLETE)
                    .addExpand("latest_invoice.payment_intent")
                    .putMetadata("cyna_user_id", userId.toString())
                    .putMetadata("cyna_order_id", orderId.toString())
                    .putMetadata("cyna_order_line_id", orderLineId.toString());
            // Stripe computes, itemises and (where applicable) reverse-charges
            // the VAT on every invoice of this subscription from the Customer's
            // address. The Customer address is set just-in-time in
            // updateCustomerTaxLocation() right before this call.
            if (properties.taxEnabled()) {
                subBuilder.setAutomaticTax(
                        SubscriptionCreateParams.AutomaticTax.builder()
                                .setEnabled(true)
                                .build());
            }
            SubscriptionCreateParams subParams = subBuilder.build();

            // Idempotency key keyed on order_line_id AND payment_method_id. A retry
            // with the SAME card replays the original result (no duplicate sub); a
            // retry with a DIFFERENT card after a decline gets a fresh key, so a
            // brand-new charge is attempted rather than Stripe replaying the
            // previously-declined `incomplete` response for 24h.
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("cyna-line-" + orderLineId + "-" + paymentMethodId)
                    .build();

            Subscription sub = Subscription.create(subParams, options);

            Instant currentPeriodEnd = extractCurrentPeriodEnd(sub);
            PaymentIntentSnapshot pi = extractPaymentIntent(sub);

            return new SubscriptionForLineResult(
                    sub.getId(),
                    sub.getStatus(),
                    currentPeriodEnd,
                    pi.status(),
                    pi.clientSecret());

        } catch (StripeException e) {
            throw new PaymentGatewayException(
                    "Stripe Subscription creation failed for line " + orderLineId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Pulls {@code current_period_end} from the just-created Subscription.
     * Stripe API 2025-04-30 moved it to {@code items.data[0]} but
     * stripe-java 26.x doesn't type-expose the new location — so we read the
     * legacy top-level field only. Returning {@code null} is safe: the caller
     * falls back to its locally-computed period and the
     * {@code customer.subscription.updated} webhook reconciles to Stripe's
     * authoritative clock (the webhook parser already handles both schemas).
     */
    private static Instant extractCurrentPeriodEnd(Subscription sub) {
        Long cpe = sub.getCurrentPeriodEnd();
        return cpe != null ? Instant.ofEpochSecond(cpe) : null;
    }

    /**
     * Extracts the PaymentIntent status + client_secret from
     * {@code latest_invoice.payment_intent} (requires {@code expand} on the
     * create call). Both are {@code null} when no PI exists (e.g. trialing sub)
     * or when the SDK schema doesn't expose them on this API version — the
     * caller treats that as "no SCA challenge available", same as today.
     */
    private static PaymentIntentSnapshot extractPaymentIntent(Subscription sub) {
        if (sub.getLatestInvoiceObject() == null) {
            return new PaymentIntentSnapshot(null, null);
        }
        Invoice invoice = sub.getLatestInvoiceObject();
        // SDK API methods covering the legacy field; newer API versions removed
        // payment_intent from invoice — we just get null then, which is fine.
        PaymentIntent pi = invoice.getPaymentIntentObject();
        if (pi == null) {
            return new PaymentIntentSnapshot(null, null);
        }
        return new PaymentIntentSnapshot(pi.getStatus(), pi.getClientSecret());
    }

    private record PaymentIntentSnapshot(String status, String clientSecret) {}

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

        ProductCreateParams.Builder productParams = ProductCreateParams.builder()
                .setName(productName)
                .putMetadata("cyna_product_id", cynaProductId.toString());
        // Per-product tax category for Stripe Tax. Products created while Tax
        // was OFF won't carry it — the Stripe account-level default tax code
        // (set in the dashboard) covers those, so calculation stays correct.
        if (properties.taxEnabled()) {
            productParams.setTaxCode(properties.taxCode());
        }

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("cyna-product-" + cynaProductId)
                .build();
        Product created = Product.create(productParams.build(), options);

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
    public void cancelSubscriptionNow(String stripeSubscriptionId) {
        try {
            Subscription stripeSub = Subscription.retrieve(stripeSubscriptionId);
            // Already terminated at Stripe — nothing to do. Keeps the
            // checkout-rollback path safe against double cancellation.
            if ("canceled".equals(stripeSub.getStatus())) {
                return;
            }
            stripeSub.cancel();
        } catch (StripeException e) {
            if ("resource_missing".equals(e.getCode())) {
                return;
            }
            throw new PaymentGatewayException(
                    "Stripe Subscription immediate cancel failed: " + e.getMessage(), e);
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
            String hostedInvoiceUrl = null;

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
                // Stripe-hosted invoice URL. Stable across API versions. Used by
                // the dunning email on `invoice.payment_action_required` so the
                // customer can complete the renewal SCA challenge directly on
                // the Stripe-hosted page (no custom 3DS handler to build).
                hostedInvoiceUrl = jsonString(obj, "hosted_invoice_url");
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
                    paymentMethodId, hostedInvoiceUrl
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
    public String createCustomerForUser(UUID userId, String email, String fullName) {
        try {
            // user_id on metadata so a Stripe dashboard operator (or a DB-loss
            // recovery script) can trace any Customer back to its Cyna user
            // without our `stripe_customers` mapping table.
            var params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(fullName)
                    .putMetadata("cyna_user_id", userId.toString())
                    .build();
            // Idempotency: a retry of the same (user) on a transient Stripe
            // failure replays the original creation instead of producing a
            // second orphan Customer.
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("cyna-user-" + userId)
                    .build();
            return Customer.create(params, options).getId();
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to create Stripe customer: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateCustomerTaxLocation(String stripeCustomerId, String paymentMethodId, String vatNumber) {
        if (!properties.taxEnabled()) {
            return;
        }
        try {
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            com.stripe.model.Address billing = pm.getBillingDetails() != null
                    ? pm.getBillingDetails().getAddress()
                    : null;
            // No usable address → don't push a partial/empty one. Stripe Tax
            // then fails closed at Subscription.create rather than us silently
            // charging a wrong (or zero) rate.
            if (billing == null || billing.getCountry() == null) {
                return;
            }
            Customer.retrieve(stripeCustomerId).update(
                    CustomerUpdateParams.builder()
                            .setAddress(
                                    CustomerUpdateParams.Address.builder()
                                            .setLine1(billing.getLine1())
                                            .setLine2(billing.getLine2())
                                            .setCity(billing.getCity())
                                            .setState(billing.getState())
                                            .setPostalCode(billing.getPostalCode())
                                            .setCountry(billing.getCountry())
                                            .build())
                            .build());
        } catch (StripeException e) {
            // Address sync is the foundation of correct VAT — fail closed so we
            // never charge against a stale/empty jurisdiction.
            throw new PaymentGatewayException(
                    "Failed to update Stripe customer tax location: " + e.getMessage(), e);
        }

        // B2B reverse charge: attach the customer's VAT number as a Stripe
        // tax_id. Best-effort (see pushVatTaxId) — must not break a valid
        // payment, since charging VAT as B2C is the safe fallback.
        if (vatNumber != null && !vatNumber.isBlank()) {
            pushVatTaxId(stripeCustomerId, vatNumber);
        }
    }

    /**
     * Attaches {@code vatNumber} to the Stripe Customer as a {@code tax_id} so
     * Stripe Tax applies the intra-EU reverse charge for valid cross-border B2B
     * numbers. Idempotent (skips if the same value is already attached) and
     * best-effort: an unrecognized prefix or a Stripe rejection (e.g. malformed
     * number) is logged and swallowed rather than failing the whole checkout.
     */
    private void pushVatTaxId(String stripeCustomerId, String rawVatNumber) {
        String vatNumber = VatNumbers.normalize(rawVatNumber);
        TaxIdCollectionCreateParams.Type type = taxIdTypeFor(vatNumber);
        if (type == null) {
            log.warn("[tax] Skipping VAT id with unrecognized country prefix for customer {} "
                    + "(value not logged)", stripeCustomerId);
            return;
        }
        try {
            // Expand tax_ids so the returned collection is bound to the
            // customer's URL (lets us both read existing ids and create new ones).
            Customer customer = Customer.retrieve(
                    stripeCustomerId,
                    CustomerRetrieveParams.builder().addExpand("tax_ids").build(),
                    null);

            boolean alreadyAttached = customer.getTaxIds() != null
                    && customer.getTaxIds().getData() != null
                    && customer.getTaxIds().getData().stream()
                    .anyMatch(t -> vatNumber.equalsIgnoreCase(t.getValue()));
            if (alreadyAttached) {
                return;
            }

            customer.getTaxIds().create(
                    TaxIdCollectionCreateParams.builder()
                            .setType(type)
                            .setValue(vatNumber)
                            .build());
        } catch (StripeException e) {
            log.warn("[tax] Could not attach VAT id to customer {} ({}). Falling back to "
                            + "standard VAT (B2C). Stripe error: {}",
                    stripeCustomerId, type, e.getMessage());
        }
    }

    /**
     * Maps a VAT number to its Stripe customer tax-id type via the shared
     * {@link VatNumbers} classification. Returns {@code null} for an unrecognized
     * prefix so the caller can skip rather than send an invalid type to Stripe.
     */
    private static TaxIdCollectionCreateParams.Type taxIdTypeFor(String vatNumber) {
        return switch (VatNumbers.regionOf(vatNumber)) {
            case EU -> TaxIdCollectionCreateParams.Type.EU_VAT;
            case GB -> TaxIdCollectionCreateParams.Type.GB_VAT;
            case CH -> TaxIdCollectionCreateParams.Type.CH_VAT;
            case NO -> TaxIdCollectionCreateParams.Type.NO_VAT;
            case UNKNOWN -> null;
        };
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
