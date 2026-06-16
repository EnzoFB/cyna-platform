package com.cyna.modules.payment.interfaces.rest.dto.response;

import com.cyna.modules.payment.application.query.previewtax.TaxPreviewReadModel;

import java.math.BigDecimal;

/**
 * Response of {@code POST /payments/tax-preview}. When {@code exact} is false,
 * Stripe Tax is off / preview unavailable and the amounts are null — the
 * frontend keeps its own estimate. {@code reverseCharge} signals the intra-EU
 * B2B autoliquidation (VAT 0%).
 */
public record TaxPreviewResponse(
        boolean exact,
        BigDecimal subtotalHt,
        BigDecimal vatAmount,
        BigDecimal totalTtc,
        String currency,
        boolean reverseCharge
) {
    public static TaxPreviewResponse from(TaxPreviewReadModel model) {
        return new TaxPreviewResponse(
                model.exact(),
                model.subtotalHt(),
                model.vatAmount(),
                model.totalTtc(),
                model.currency(),
                model.reverseCharge());
    }
}
