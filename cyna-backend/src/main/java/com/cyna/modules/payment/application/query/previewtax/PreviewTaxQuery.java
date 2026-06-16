package com.cyna.modules.payment.application.query.previewtax;

import com.cyna.shared.application.Query;

import java.util.List;
import java.util.UUID;

/**
 * Asks for the exact VAT of a prospective checkout (before any charge) so the
 * UI can show the authoritative amount, including a B2B reverse charge. Prices
 * are resolved server-side from {@code productId} — the client never dictates
 * the amount taxed.
 */
public record PreviewTaxQuery(
        String currency,
        List<Line> lines,
        String countryCode,
        String postalCode,
        String state,
        String vatNumber
) implements Query<TaxPreviewReadModel> {

    public record Line(UUID productId, String billingCycle, int quantity) {}
}
