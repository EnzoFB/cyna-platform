package com.cyna.modules.payment.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FinalizePaymentRequest(
        @NotNull(message = "orderId is required")
        UUID orderId,

        @NotBlank(message = "paymentMethodId is required")
        String paymentMethodId,

        // Optional B2B VAT number. When present and valid for a cross-border EU
        // customer, Stripe Tax applies the reverse charge (0% VAT). Null/blank
        // for B2C — standard destination VAT applies.
        String vatNumber,

        // UI language at checkout time — used to localise the confirmation email.
        String lang
) {}
