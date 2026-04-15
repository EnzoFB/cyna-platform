package com.cyna.modules.order.infrastructure.persistence.repository;

import com.cyna.modules.order.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {
    @EntityGraph(attributePaths = "lines")
    Page<OrderJpaEntity> findByUserId(UUID userId, Pageable pageable);
}
