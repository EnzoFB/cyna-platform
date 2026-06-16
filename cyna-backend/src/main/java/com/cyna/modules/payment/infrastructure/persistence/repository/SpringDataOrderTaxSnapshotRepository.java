package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.infrastructure.persistence.entity.OrderTaxSnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataOrderTaxSnapshotRepository extends JpaRepository<OrderTaxSnapshotJpaEntity, UUID> {
    Optional<OrderTaxSnapshotJpaEntity> findByOrderIdAndUserId(UUID orderId, UUID userId);
}
