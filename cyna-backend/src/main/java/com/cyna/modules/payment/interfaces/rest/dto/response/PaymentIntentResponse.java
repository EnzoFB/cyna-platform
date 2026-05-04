package com.cyna.modules.payment.interfaces.rest.dto.response;

import com.cyna.modules.payment.application.model.PaymentInitiatedReadModel;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentIntentResponse(
        UUID paymentId,
        UUID orderId,
        String clientSecret,
        BigDecimal amount,
        String currency
) {
    public static PaymentIntentResponse from(PaymentInitiatedReadModel model) {
        return new PaymentIntentResponse(
                model.paymentId(),
                model.orderId(),
                model.clientSecret(),
                model.amount(),
                model.currency()
        );
    }
}
