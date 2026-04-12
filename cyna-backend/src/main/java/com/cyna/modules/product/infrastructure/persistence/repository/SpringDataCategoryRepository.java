package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataCategoryRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    Optional<CategoryJpaEntity> findByName(String name);

    boolean existsByName(String name);
}
