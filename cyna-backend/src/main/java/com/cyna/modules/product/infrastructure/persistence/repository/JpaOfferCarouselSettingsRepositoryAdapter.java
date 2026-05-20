package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.domain.model.CarouselSettingsTranslation;
import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.modules.product.infrastructure.persistence.entity.OfferCarouselSettingsJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.OfferCarouselSettingsTranslationJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        entity.setCreatedAt(settings.getCreatedAt());
        entity.setUpdatedAt(settings.getUpdatedAt());

        Set<OfferCarouselSettingsTranslationJpaEntity> translations = new HashSet<>();
        settings.getTranslations().forEach((locale, t) ->
                translations.add(OfferCarouselSettingsTranslationJpaEntity.of(entity, locale, t.fixedText()))
        );
        entity.setTranslations(translations);

        springRepository.save(entity);
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private OfferCarouselSettings toDomain(OfferCarouselSettingsJpaEntity entity) {
        Map<String, CarouselSettingsTranslation> translations = new HashMap<>();
        entity.getTranslations().forEach(t ->
                translations.put(t.getLocale(), new CarouselSettingsTranslation(t.getFixedText()))
        );

        return OfferCarouselSettings.reconstitute(
                translations,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
