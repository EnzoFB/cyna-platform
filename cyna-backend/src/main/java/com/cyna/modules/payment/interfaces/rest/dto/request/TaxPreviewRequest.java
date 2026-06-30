package com.cyna.modules.payment.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Body of {@code POST /payments/tax-preview}. Carries the prospective cart lines
 * (by productId — prices are resolved server-side) plus the billing location and
 * optional B2B VAT number, so Stripe Tax can compute the exact VAT to display.
 */
public record TaxPreviewRequest(
        @NotBlank(message = "currency is required")
        String currency,

        @NotEmpty(message = "lines must not be empty")
        List<Line> lines,

        @NotBlank(message = "countryCode is required")
        String countryCode,

        String postalCode,
        String state,
        String vatNumber
) {
    public record Line(UUID productId, String billingCycle, int quantity) {}
}
