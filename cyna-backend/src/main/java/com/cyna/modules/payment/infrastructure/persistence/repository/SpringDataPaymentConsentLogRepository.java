package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.infrastructure.persistence.entity.PaymentConsentLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataPaymentConsentLogRepository
        extends JpaRepository<PaymentConsentLogJpaEntity, UUID> {

    List<PaymentConsentLogJpaEntity> findByUserIdOrderByGivenAtDesc(UUID userId);
}
