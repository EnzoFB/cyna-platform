package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, UUID> {

    List<ProductJpaEntity> findAllByIdIn(Collection<UUID> ids);
}
