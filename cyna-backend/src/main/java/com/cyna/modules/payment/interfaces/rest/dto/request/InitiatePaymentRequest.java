package com.cyna.modules.payment.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InitiatePaymentRequest(
        @NotNull(message = "orderId is required")
        UUID orderId
) {}
