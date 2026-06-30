package com.cyna.modules.payment.application.query.previewtax;

import java.math.BigDecimal;

/**
 * Result of a tax preview. When {@code exact} is false, Stripe Tax is off (or
 * the preview could not be computed) and the amount fields are null — the
 * frontend then keeps its own client-side estimate. {@code reverseCharge} is
 * true when the intra-EU B2B autoliquidation applies (VAT 0%, due by the buyer).
 */
public record TaxPreviewReadModel(
        boolean exact,
        BigDecimal subtotalHt,
        BigDecimal vatAmount,
        BigDecimal totalTtc,
        String currency,
        boolean reverseCharge
) {
    /** Stripe Tax disabled or preview unavailable — frontend keeps its estimate. */
    public static TaxPreviewReadModel estimateFallback(String currency) {
        return new TaxPreviewReadModel(false, null, null, null, currency, false);
    }
}
