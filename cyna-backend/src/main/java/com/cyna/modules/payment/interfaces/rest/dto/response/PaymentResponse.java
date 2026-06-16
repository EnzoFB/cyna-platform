package com.cyna.modules.payment.interfaces.rest.dto.response;

import com.cyna.modules.payment.application.query.getbyid.PaymentReadModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String status,
        BigDecimal amount,
        String currency,
        String stripePaymentIntentId,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(PaymentReadModel model) {
        return new PaymentResponse(
                model.id(),
                model.orderId(),
                model.status(),
                model.amount(),
                model.currency(),
                model.stripePaymentIntentId(),
                model.createdAt(),
                model.updatedAt()
        );
    }
}
