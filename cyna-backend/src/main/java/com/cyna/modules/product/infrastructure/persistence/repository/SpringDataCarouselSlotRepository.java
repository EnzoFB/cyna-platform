package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.infrastructure.persistence.entity.CarouselSlotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SpringDataCarouselSlotRepository extends JpaRepository<CarouselSlotJpaEntity, UUID> {

    List<CarouselSlotJpaEntity> findAllByOrderBySlotOrderAsc();

    boolean existsByPromotionId(UUID promotionId);

    void deleteByPromotionId(UUID promotionId);

    @Modifying
    @Query("DELETE FROM CarouselSlotJpaEntity s WHERE s.promotionId IN :ids")
    void deleteAllByPromotionIdIn(@Param("ids") Collection<UUID> ids);

    @Query("SELECT COALESCE(MAX(s.slotOrder), 0) FROM CarouselSlotJpaEntity s")
    int findMaxSlotOrder();
}
