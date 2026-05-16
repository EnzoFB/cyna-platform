package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.infrastructure.persistence.entity.StripeCustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import java.util.Optional;

interface SpringDataStripeCustomerRepository extends JpaRepository<StripeCustomerJpaEntity, UUID> {
    Optional<StripeCustomerJpaEntity> findByStripeCustomerId(String stripeCustomerId);
}
