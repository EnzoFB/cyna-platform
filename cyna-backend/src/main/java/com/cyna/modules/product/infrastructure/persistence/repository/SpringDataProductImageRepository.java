package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.infrastructure.persistence.entity.ProductImageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SpringDataProductImageRepository extends JpaRepository<ProductImageJpaEntity, UUID> {

    List<ProductImageJpaEntity> findAllByProduct_IdOrderByDisplayOrderAsc(UUID productId);

    List<ProductImageJpaEntity> findAllByProduct_IdInOrderByProduct_IdAscDisplayOrderAsc(Collection<UUID> productIds);

    int countByProduct_Id(UUID productId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(MAX(e.displayOrder), -1) FROM ProductImageJpaEntity e WHERE e.product.id = :productId"
    )
    int findMaxDisplayOrderByProductId(@org.springframework.data.repository.query.Param("productId") UUID productId);
}
