package com.cyna.modules.payment.domain.port;

import java.math.BigDecimal;
import java.util.List;

/**
 * Computes the <b>exact</b> VAT of a prospective checkout (no charge, nothing
 * persisted) so the UI can show the authoritative amount — including the
 * intra-EU B2B reverse charge — before the customer pays.
 *
 * <p>Kept separate from {@link PaymentGatewayPort} (single-responsibility): the
 * tax preview is a read-only calculation, not a payment mutation, and its only
 * consumer is the checkout preview query.
 */
public interface TaxCalculationPort {

    /**
     * Returns {@link TaxCalculationResult#disabled()} (i.e. {@code enabled=false})
     * when Stripe Tax is turned off, so the caller can fall back to its own
     * estimate instead of erroring.
     */
    TaxCalculationResult calculateTax(TaxCalculationRequest request);

    /**
     * Inputs for a tax preview. Amounts are tax-exclusive (HT) unit prices in the
     * catalog currency — Stripe adds the VAT on top, matching the
     * {@code tax_behavior=exclusive} used when subscriptions are created.
     */
    record TaxCalculationRequest(
            String currency,
            List<TaxLine> lines,
            String countryCode,
            String postalCode,
            String state,
            String vatNumber
    ) {
        public record TaxLine(String reference, BigDecimal unitAmountHt, int quantity) {}
    }

    /**
     * Result of a tax preview. When {@code enabled} is false the amount fields are
     * null. {@code reverseCharge} is true when Stripe applied the intra-EU
     * autoliquidation (0% VAT, due by the customer).
     */
    record TaxCalculationResult(
            boolean enabled,
            BigDecimal subtotalHt,
            BigDecimal vatAmount,
            BigDecimal totalTtc,
            String currency,
            boolean reverseCharge
    ) {
        public static TaxCalculationResult disabled() {
            return new TaxCalculationResult(false, null, null, null, null, false);
        }
    }
}
