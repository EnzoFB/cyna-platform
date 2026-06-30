package com.cyna.modules.payment.application.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only cross-module access to a user's payment-area personal data.
 * Currently exposes the recorded consents for the RGPD Art. 15/20 export
 * (transparency: the user can see exactly what they consented to, when, and
 * from where).
 */
public interface PaymentQueryApi {

    List<PaymentConsentExportView> exportConsentsForUser(UUID userId);

    record PaymentConsentExportView(
            String action,
            String labelVersion,
            String stripePaymentMethodId,
            String ipAddress,
            String userAgent,
            Instant givenAt
    ) {}

    /**
     * The authoritative VAT/TTC for one of the user's orders, read live from the
     * Stripe invoices of its subscriptions. Returns an {@link OrderTaxSummaryView}
     * with {@code available=false} when the order has no invoiced subscription yet
     * or Stripe can't be reached, so callers (the order confirmation page and the
     * confirmation email) gracefully fall back to the HT subtotal. Scoped by
     * {@code userId}: another user's order resolves to no subscriptions.
     */
    OrderTaxSummaryView getOrderTaxSummary(UUID userId, UUID orderId);

    record OrderTaxSummaryView(
            boolean available,
            BigDecimal subtotalHt,
            BigDecimal vatAmount,
            BigDecimal totalTtc,
            String currency,
            boolean reverseCharge
    ) {
        public static OrderTaxSummaryView unavailable() {
            return new OrderTaxSummaryView(false, null, null, null, null, false);
        }
    }
}
