package com.cyna.modules.subscription.application.query.getbyid;

import com.cyna.modules.subscription.domain.model.Subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionReadModel(
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
        Instant createdAt,
        Instant updatedAt
) {
    public static SubscriptionReadModel from(Subscription subscription) {
        return new SubscriptionReadModel(
                subscription.getId(),
                subscription.getUserId(),
                subscription.getOrderId(),
                subscription.getProductId(),
                subscription.getProductName(),
                subscription.getProductCategory(),
                subscription.getBillingCycle().name(),
                subscription.getStatus().name(),
                subscription.getQuantity(),
                subscription.getUnitPrice().amount(),
                subscription.getUnitPrice().currency(),
                subscription.getStartAt(),
                subscription.getEndAt(),
                subscription.getNextBillingAt(),
                subscription.getCancelledAt(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }
}
