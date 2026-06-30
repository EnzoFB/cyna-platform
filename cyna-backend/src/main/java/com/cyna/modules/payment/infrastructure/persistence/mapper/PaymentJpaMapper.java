package com.cyna.modules.payment.infrastructure.persistence.mapper;

import com.cyna.modules.payment.domain.model.Payment;
import com.cyna.modules.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import com.cyna.shared.domain.Money;
import org.springframework.stereotype.Component;

@Component
public class PaymentJpaMapper {

    public PaymentJpaEntity toEntity(Payment payment) {
        var entity = new PaymentJpaEntity();
        entity.setId(payment.getId());
        entity.setOrderId(payment.getOrderId());
        entity.setUserId(payment.getUserId());
        entity.setStatus(payment.getStatus());
        entity.setAmount(payment.getAmount().amount());
        entity.setCurrency(payment.getAmount().currency());
        entity.setStripeSetupIntentId(payment.getStripeSetupIntentId());
        entity.setStripeSetupIntentClientSecret(payment.getStripeSetupIntentClientSecret());
        entity.setStripePaymentIntentId(payment.getStripePaymentIntentId());
        entity.setStripeClientSecret(payment.getStripeClientSecret());
        entity.setStripeSubscriptionId(payment.getStripeSubscriptionId());
        entity.setStripeScheduleId(payment.getStripeScheduleId());
        entity.setCreatedAt(payment.getCreatedAt());
        entity.setUpdatedAt(payment.getUpdatedAt());
        return entity;
    }

    public Payment toDomain(PaymentJpaEntity entity) {
        return Payment.reconstitute(
                entity.getId(),
                entity.getOrderId(),
                entity.getUserId(),
                entity.getStatus(),
                Money.of(entity.getAmount(), entity.getCurrency()),
                entity.getStripeSetupIntentId(),
                entity.getStripeSetupIntentClientSecret(),
                entity.getStripePaymentIntentId(),
                entity.getStripeClientSecret(),
                entity.getStripeSubscriptionId(),
                entity.getStripeScheduleId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
