package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.infrastructure.persistence.entity.OfferCarouselSettingsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOfferCarouselSettingsRepository extends JpaRepository<OfferCarouselSettingsJpaEntity, Short> {
}
