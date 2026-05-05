package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.infrastructure.persistence.entity.StripeProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataStripeProductRepository extends JpaRepository<StripeProductJpaEntity, UUID> {}
