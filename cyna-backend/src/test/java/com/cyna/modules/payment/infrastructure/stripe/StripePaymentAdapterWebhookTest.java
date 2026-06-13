package com.cyna.modules.payment.infrastructure.stripe;

import com.cyna.modules.payment.config.StripeProperties;
import com.cyna.modules.payment.domain.port.PaymentGatewayPort.StripeWebhookEvent;
import com.cyna.modules.payment.domain.port.WebhookSignatureException;
import com.cyna.modules.payment.domain.repository.StripeProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit coverage for {@link StripePaymentAdapter#parseWebhookEvent} — the single
 * piece of code that touches the raw Stripe contract. It is deliberately
 * version-agnostic (parses raw JSON because typed SDK deserialization breaks
 * when Stripe's account API version drifts from the pinned SDK), so the
 * extraction branches and the 2024↔2025 schema fallbacks are exactly the
 * fragile, business-critical code: a regression there silently breaks
 * renewals / cancellations. We feed real Stripe event envelopes signed with a
 * valid HMAC so {@code Webhook.constructEvent} accepts them — no Docker, no
 * network, fully deterministic.
 */
class StripePaymentAdapterWebhookTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret_for_unit";

    private StripePaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        StripeProperties props = new StripeProperties(
                "sk_test_dummy", "pk_test_dummy", WEBHOOK_SECRET,
                false, "txcd_10103000");
        adapter = new StripePaymentAdapter(props, mock(StripeProductRepository.class));
    }

    // ---- Field extraction --------------------------------------------------

    @Test
    void parses_first_invoice_paid_subscription_create() {
        String payload = fixture("invoice-paid-create.json");

        StripeWebhookEvent e = adapter.parseWebhookEvent(payload, signedHeader(payload));

        assertThat(e.eventId()).isEqualTo("evt_invoice_paid_create"); // dedup key
        assertThat(e.type()).isEqualTo("invoice.paid");
        assertThat(e.billingReason()).isEqualTo("subscription_create");
        assertThat(e.subscriptionId()).isEqualTo("sub_CREATE1");
        assertThat(e.paymentIntentId()).isEqualTo("pi_CREATE1");
        assertThat(e.customerId()).isEqualTo("cus_ABC");
        assertThat(e.periodEnd()).isEqualTo(Instant.ofEpochSecond(1767225600L));
    }

    @Test
    void reads_subscription_id_from_parent_in_2025_invoice_schema() {
        // 2025-03-31.basil moved invoice.subscription under invoice.parent.subscription.
        // If this fallback regresses, renewals can no longer be matched to a sub.
        String payload = fixture("invoice-paid-cycle-2025.json");

        StripeWebhookEvent e = adapter.parseWebhookEvent(payload, signedHeader(payload));

        assertThat(e.type()).isEqualTo("invoice.paid");
        assertThat(e.billingReason()).isEqualTo("subscription_cycle");
        assertThat(e.subscriptionId()).isEqualTo("sub_CYCLE1"); // from parent.subscription
        assertThat(e.periodEnd()).isEqualTo(Instant.ofEpochSecond(1769904000L));
    }

    @Test
    void parses_subscription_updated_with_legacy_2024_top_level_fields() {
        String payload = fixture("subscription-updated-2024.json");

        StripeWebhookEvent e = adapter.parseWebhookEvent(payload, signedHeader(payload));

        assertThat(e.type()).isEqualTo("customer.subscription.updated");
        assertThat(e.subscriptionId()).isEqualTo("sub_2024");
        assertThat(e.subscriptionStatus()).isEqualTo("active");
        assertThat(e.cancelAtPeriodEnd()).isTrue();
        assertThat(e.currentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(1767225600L));
        assertThat(e.canceledAt()).isEqualTo(Instant.ofEpochSecond(1764547200L));
    }

    @Test
    void reads_current_period_end_from_items_in_2025_subscription_schema() {
        // 2025-04-30 moved current_period_end under items.data[0].current_period_end.
        // A regression here would freeze every subscription's period mirror.
        String payload = fixture("subscription-updated-2025.json");

        StripeWebhookEvent e = adapter.parseWebhookEvent(payload, signedHeader(payload));

        assertThat(e.subscriptionId()).isEqualTo("sub_2025");
        assertThat(e.subscriptionStatus()).isEqualTo("past_due");
        assertThat(e.cancelAtPeriodEnd()).isFalse();
        assertThat(e.currentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(1772582400L));
    }

    @Test
    void parses_payment_method_attached() {
        String payload = fixture("payment-method-attached.json");

        StripeWebhookEvent e = adapter.parseWebhookEvent(payload, signedHeader(payload));

        assertThat(e.type()).isEqualTo("payment_method.attached");
        assertThat(e.paymentMethodId()).isEqualTo("pm_ATT1");
        assertThat(e.customerId()).isEqualTo("cus_ABC");
    }

    @Test
    void parses_payment_intent_succeeded() {
        String payload = fixture("payment-intent-succeeded.json");

        StripeWebhookEvent e = adapter.parseWebhookEvent(payload, signedHeader(payload));

        assertThat(e.type()).isEqualTo("payment_intent.succeeded");
        assertThat(e.paymentIntentId()).isEqualTo("pi_OK1");
    }

    // ---- Signature security boundary --------------------------------------

    @Test
    void rejects_a_tampered_payload() {
        String payload = fixture("invoice-paid-create.json");
        String header = signedHeader(payload);
        String tampered = payload.replace("sub_CREATE1", "sub_ATTACKER");

        assertThatThrownBy(() -> adapter.parseWebhookEvent(tampered, header))
                .isInstanceOf(WebhookSignatureException.class);
    }

    @Test
    void rejects_a_payload_signed_with_the_wrong_secret() {
        String payload = fixture("invoice-paid-create.json");
        String header = signedHeaderWith(payload, "whsec_wrong_secret");

        assertThatThrownBy(() -> adapter.parseWebhookEvent(payload, header))
                .isInstanceOf(WebhookSignatureException.class);
    }

    // ---- Helpers -----------------------------------------------------------

    private String signedHeader(String payload) {
        return signedHeaderWith(payload, WEBHOOK_SECRET);
    }

    /** Builds a Stripe-style {@code t=<ts>,v1=<hmac>} header (HMAC-SHA256 of "ts.payload"). */
    private String signedHeaderWith(String payload, String secret) {
        long ts = Instant.now().getEpochSecond();
        String signedPayload = ts + "." + payload;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return "t=" + ts + ",v1=" + hex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign test payload", ex);
        }
    }

    private String fixture(String name) {
        try (var in = getClass().getResourceAsStream("/stripe/" + name)) {
            if (in == null) {
                throw new IllegalStateException("Missing test fixture: /stripe/" + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load fixture " + name, ex);
        }
    }
}
