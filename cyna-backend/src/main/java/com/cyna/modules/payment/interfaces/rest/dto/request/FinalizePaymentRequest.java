package com.cyna.modules.payment.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FinalizePaymentRequest(
        @NotNull(message = "orderId is required")
        UUID orderId,

        @NotBlank(message = "paymentMethodId is required")
        String paymentMethodId
) {}
