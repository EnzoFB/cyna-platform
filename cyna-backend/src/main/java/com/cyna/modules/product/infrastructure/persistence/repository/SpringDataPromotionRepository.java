package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.infrastructure.persistence.entity.PromotionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SpringDataPromotionRepository extends JpaRepository<PromotionJpaEntity, UUID> {

    List<PromotionJpaEntity> findAllByProduct_IdIn(Collection<UUID> productIds);

    List<PromotionJpaEntity> findAllByShowInCarouselTrue();

    /**
     * Clears carousel state on the given promotions so new orders can be assigned without
     * violating the unique + check constraints. showInCarousel is also set to false so the
     * check constraint (carousel_order NOT NULL when show_in_carousel = TRUE) is satisfied.
     * The second pass in saveAll() will restore showInCarousel=true with the new order.
     */
    @Modifying
    @Query("UPDATE PromotionJpaEntity p SET p.carouselOrder = NULL, p.showInCarousel = FALSE WHERE p.id IN :ids")
    void clearCarouselOrders(@Param("ids") Collection<UUID> ids);

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

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM PromotionJpaEntity p
            WHERE p.showInCarousel = true
              AND p.carouselOrder = :carouselOrder
              AND (:excludedId IS NULL OR p.id <> :excludedId)
            """)
    boolean existsCarouselOrder(@Param("carouselOrder") Integer carouselOrder,
                                @Param("excludedId") UUID excludedId);

    @Query("""
            SELECT COUNT(p)
            FROM PromotionJpaEntity p
            WHERE p.showInCarousel = true
              AND (:excludedId IS NULL OR p.id <> :excludedId)
            """)
    long countVisibleInCarousel(@Param("excludedId") UUID excludedId);
}
