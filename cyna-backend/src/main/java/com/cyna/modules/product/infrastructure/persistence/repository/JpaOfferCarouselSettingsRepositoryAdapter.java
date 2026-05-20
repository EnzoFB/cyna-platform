package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.modules.product.infrastructure.persistence.entity.OfferCarouselSettingsJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaOfferCarouselSettingsRepositoryAdapter implements OfferCarouselSettingsRepository {

    private static final short SINGLETON_ID = 1;

    private final SpringDataOfferCarouselSettingsRepository springRepository;

    public JpaOfferCarouselSettingsRepositoryAdapter(SpringDataOfferCarouselSettingsRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Optional<OfferCarouselSettings> find() {
        return springRepository.findById(SINGLETON_ID).map(this::toDomain);
    }

    @Override
    public void save(OfferCarouselSettings settings) {
        OfferCarouselSettingsJpaEntity entity = springRepository.findById(SINGLETON_ID)
                .orElseGet(OfferCarouselSettingsJpaEntity::new);
        entity.setId(SINGLETON_ID);
        entity.setFixedTextFr(settings.getFixedTextFr());
        entity.setFixedTextEn(settings.getFixedTextEn());
        entity.setCreatedAt(settings.getCreatedAt());
        entity.setUpdatedAt(settings.getUpdatedAt());
        springRepository.save(entity);
    }

    private OfferCarouselSettings toDomain(OfferCarouselSettingsJpaEntity entity) {
        return OfferCarouselSettings.reconstitute(
                entity.getFixedTextFr(),
                entity.getFixedTextEn(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
