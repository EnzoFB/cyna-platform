package com.cyna.modules.payment.application.model;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentInitiatedReadModel(
        UUID paymentId,
        UUID orderId,
        String clientSecret,
        BigDecimal amount,
        String currency
) {}
