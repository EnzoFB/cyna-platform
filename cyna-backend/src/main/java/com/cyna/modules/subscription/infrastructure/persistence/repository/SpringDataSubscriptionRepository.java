package com.cyna.modules.subscription.infrastructure.persistence.repository;

import com.cyna.modules.subscription.infrastructure.persistence.entity.SubscriptionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataSubscriptionRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
    Page<SubscriptionJpaEntity> findByUserId(UUID userId, Pageable pageable);
    List<SubscriptionJpaEntity> findAllByStripeSubscriptionId(String stripeSubscriptionId);

    @Query("SELECT s FROM SubscriptionJpaEntity s WHERE s.status = 'ACTIVE' AND s.autoRenew = true AND s.autoRenewNoticeSentAt IS NULL AND s.endAt >= :fromInclusive AND s.endAt < :toExclusive")
    List<SubscriptionJpaEntity> findActiveAutoRenewDueForNotice(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );
}
