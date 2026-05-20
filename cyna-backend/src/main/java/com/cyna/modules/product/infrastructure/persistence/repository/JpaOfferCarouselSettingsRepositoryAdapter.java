package com.cyna.modules.product.infrastructure.persistence.repository;

import com.cyna.modules.product.domain.model.OfferCarouselSettings;
import com.cyna.modules.product.domain.repository.OfferCarouselSettingsRepository;
import com.cyna.modules.product.infrastructure.persistence.entity.OfferCarouselSettingsJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.OfferCarouselSettingsTranslationJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
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

        // Build translation rows
        Set<OfferCarouselSettingsTranslationJpaEntity> translations = new HashSet<>();
        translations.add(OfferCarouselSettingsTranslationJpaEntity.of(entity, "fr", settings.getFixedTextFr()));
        if (settings.getFixedTextEn() != null && !settings.getFixedTextEn().isBlank()) {
            translations.add(OfferCarouselSettingsTranslationJpaEntity.of(entity, "en", settings.getFixedTextEn()));
        }
        entity.setTranslations(translations);

        springRepository.save(entity);
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private OfferCarouselSettings toDomain(OfferCarouselSettingsJpaEntity entity) {
        OfferCarouselSettingsTranslationJpaEntity fr = findLocale(entity, "fr");
        OfferCarouselSettingsTranslationJpaEntity en = findLocale(entity, "en");

        String fixedTextFr = fr != null ? fr.getFixedText() : "";
        String fixedTextEn = en != null ? en.getFixedText() : "";

        return OfferCarouselSettings.reconstitute(
                fixedTextFr,
                fixedTextEn,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static OfferCarouselSettingsTranslationJpaEntity findLocale(
            OfferCarouselSettingsJpaEntity entity, String locale) {
        return entity.getTranslations().stream()
                .filter(t -> locale.equals(t.getLocale()))
                .findFirst()
                .orElse(null);
    }
}
