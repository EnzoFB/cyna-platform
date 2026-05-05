package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {
    Optional<PaymentJpaEntity> findByOrderId(UUID orderId);
    Optional<PaymentJpaEntity> findByStripePaymentIntentId(String stripePaymentIntentId);
}
