package com.cyna.modules.subscription.infrastructure.persistence.repository;

import com.cyna.modules.subscription.infrastructure.persistence.entity.SubscriptionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataSubscriptionRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
    Page<SubscriptionJpaEntity> findByUserId(UUID userId, Pageable pageable);
    List<SubscriptionJpaEntity> findAllByStripeSubscriptionId(String stripeSubscriptionId);
    List<SubscriptionJpaEntity> findByStatusAndAutoRenewIsTrueAndAutoRenewNoticeSentAtIsNullAndEndAtGreaterThanEqualAndEndAtLessThan(
            String status,
            Instant fromInclusive,
            Instant toExclusive
    );
}
