package com.cyna.modules.payment.application.query.getbyid;

import com.cyna.modules.payment.domain.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReadModel(
        UUID id,
        UUID orderId,
        UUID userId,
        String status,
        BigDecimal amount,
        String currency,
        String stripePaymentIntentId,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentReadModel from(Payment payment) {
        return new PaymentReadModel(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getStatus().name(),
                payment.getAmount().amount(),
                payment.getAmount().currency(),
                payment.getStripePaymentIntentId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
