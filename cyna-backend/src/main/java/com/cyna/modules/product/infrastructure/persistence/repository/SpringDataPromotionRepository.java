package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.infrastructure.persistence.entity.PromotionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SpringDataPromotionRepository extends JpaRepository<PromotionJpaEntity, UUID> {

    List<PromotionJpaEntity> findAllByProduct_IdIn(Collection<UUID> productIds);

    @Query("""
            SELECT p
            FROM PromotionJpaEntity p
            WHERE p.product.id IN :productIds
              AND p.enabled = true
              AND p.startAt <= :atInstant
              AND p.endAt > :atInstant
            """)
    List<PromotionJpaEntity> findActiveByProductIds(@Param("productIds") Collection<UUID> productIds,
                                                    @Param("atInstant") Instant atInstant);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM PromotionJpaEntity p
            WHERE p.product.id = :productId
              AND p.enabled = true
              AND p.startAt < :endAt
              AND p.endAt > :startAt
              AND (:excludedId IS NULL OR p.id <> :excludedId)
            """)
    boolean existsEnabledOverlappingWindow(@Param("productId") UUID productId,
                                           @Param("startAt") Instant startAt,
                                           @Param("endAt") Instant endAt,
                                           @Param("excludedId") UUID excludedId);
}

