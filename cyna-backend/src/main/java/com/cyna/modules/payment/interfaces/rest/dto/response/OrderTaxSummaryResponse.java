package com.cyna.modules.payment.interfaces.rest.dto.response;

import com.cyna.modules.payment.application.api.PaymentQueryApi.OrderTaxSummaryView;

import java.math.BigDecimal;

/**
 * Authoritative VAT/TTC for a paid order, read live from its Stripe invoices.
 * When {@code available} is false (invoice not ready yet, or Stripe unreachable),
 * the amount fields are null and the client falls back to the HT subtotal.
 */
public record OrderTaxSummaryResponse(
        boolean available,
        BigDecimal subtotalHt,
        BigDecimal vatAmount,
        BigDecimal totalTtc,
        String currency,
        boolean reverseCharge
) {
    public static OrderTaxSummaryResponse from(OrderTaxSummaryView v) {
        return new OrderTaxSummaryResponse(
                v.available(),
                v.subtotalHt(),
                v.vatAmount(),
                v.totalTtc(),
                v.currency(),
                v.reverseCharge());
    }
}
