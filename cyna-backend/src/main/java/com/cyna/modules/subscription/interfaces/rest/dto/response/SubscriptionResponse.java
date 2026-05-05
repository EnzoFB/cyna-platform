package com.cyna.modules.subscription.interfaces.rest.dto.response;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID userId,
        UUID orderId,
        UUID productId,
        String productName,
        String productCategory,
        String billingCycle,
        String status,
        int quantity,
        BigDecimal unitPrice,
        String currency,
        Instant startAt,
        Instant endAt,
        Instant nextBillingAt,
        Instant cancelledAt,
        boolean autoRenew,
        Instant autoRenewNoticeSentAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static SubscriptionResponse from(SubscriptionReadModel model) {
        return new SubscriptionResponse(
                model.id(),
                model.userId(),
                model.orderId(),
                model.productId(),
                model.productName(),
                model.productCategory(),
                model.billingCycle(),
                model.status(),
                model.quantity(),
                model.unitPrice(),
                model.currency(),
                model.startAt(),
                model.endAt(),
                model.nextBillingAt(),
                model.cancelledAt(),
                model.autoRenew(),
                model.autoRenewNoticeSentAt(),
                model.createdAt(),
                model.updatedAt()
        );
    }
}
