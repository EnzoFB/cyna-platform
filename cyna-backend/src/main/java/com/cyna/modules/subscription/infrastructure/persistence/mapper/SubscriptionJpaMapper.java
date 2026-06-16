package com.cyna.modules.subscription.infrastructure.persistence.mapper;

import com.cyna.shared.domain.BillingCycle;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.modules.subscription.domain.model.SubscriptionStatus;
import com.cyna.modules.subscription.infrastructure.persistence.entity.SubscriptionJpaEntity;
import com.cyna.shared.domain.Money;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionJpaMapper {

    public SubscriptionJpaEntity toJpa(Subscription subscription) {
        SubscriptionJpaEntity entity = new SubscriptionJpaEntity();
        entity.setId(subscription.getId());
        entity.setUserId(subscription.getUserId());
        entity.setOrderId(subscription.getOrderId());
        entity.setOrderLineId(subscription.getOrderLineId());
        entity.setProductId(subscription.getProductId());
        entity.setProductName(subscription.getProductName());
        entity.setProductCategory(subscription.getProductCategory());
        entity.setBillingCycle(subscription.getBillingCycle().name());
        entity.setStatus(subscription.getStatus().name());
        entity.setQuantity(subscription.getQuantity());
        entity.setUnitPrice(subscription.getUnitPrice().amount());
        entity.setCurrency(subscription.getUnitPrice().currency());
        entity.setStartAt(subscription.getStartAt());
        entity.setEndAt(subscription.getEndAt());
        entity.setNextBillingAt(subscription.getNextBillingAt());
        entity.setCancelledAt(subscription.getCancelledAt());
        entity.setAutoRenew(subscription.isAutoRenew());
        entity.setAutoRenewNoticeSentAt(subscription.getAutoRenewNoticeSentAt());
        entity.setStripeSubscriptionId(subscription.getStripeSubscriptionId());
        entity.setStripeScheduleId(subscription.getStripeScheduleId());
        entity.setCreatedAt(subscription.getCreatedAt());
        entity.setUpdatedAt(subscription.getUpdatedAt());
        return entity;
    }

    public Subscription toDomain(SubscriptionJpaEntity entity) {
        return Subscription.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getOrderId(),
                entity.getOrderLineId(),
                entity.getProductId(),
                entity.getProductName(),
                entity.getProductCategory(),
                BillingCycle.valueOf(entity.getBillingCycle()),
                SubscriptionStatus.valueOf(entity.getStatus()),
                entity.getQuantity(),
                Money.of(entity.getUnitPrice(), entity.getCurrency()),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getNextBillingAt(),
                entity.getCancelledAt(),
                entity.isAutoRenew(),
                entity.getAutoRenewNoticeSentAt(),
                entity.getStripeSubscriptionId(),
                entity.getStripeScheduleId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
