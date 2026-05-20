package com.cyna.modules.product.infrastructure.persistence.mapper;

import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryTranslationJpaEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class CategoryJpaMapper {

    // ── Domain → JPA ─────────────────────────────────────────────────────────

    public CategoryJpaEntity toJpa(Category category) {
        var entity = new CategoryJpaEntity();

        entity.setId(category.getId());
        entity.setName(category.getName());
        entity.setImage(category.getImage());
        entity.setActive(category.isActive());
        entity.setCreatedAt(category.getCreatedAt());
        entity.setUpdatedAt(category.getUpdatedAt());

        // Build translation rows
        Set<CategoryTranslationJpaEntity> translations = new HashSet<>();

        translations.add(CategoryTranslationJpaEntity.of(
            entity,
            "fr",
            category.getFullName(),
            category.getDescription()
        ));

        // EN row only when there is actual EN content
        if (hasEnContent(category)) {
            translations.add(CategoryTranslationJpaEntity.of(
                entity,
                "en",
                blankOr(category.getFullNameEn(),    category.getFullName()),
                blankOr(category.getDescriptionEn(), category.getDescription())
            ));
        }

        entity.setTranslations(translations);
        return entity;
    }

    // ── JPA → Domain ─────────────────────────────────────────────────────────

    public Category toDomain(CategoryJpaEntity entity) {
        CategoryTranslationJpaEntity fr = findLocale(entity, "fr");
        CategoryTranslationJpaEntity en = findLocale(entity, "en");

        String fullName      = fr != null ? fr.getFullName()    : "";
        String description   = fr != null ? fr.getDescription() : "";
        String fullNameEn    = en != null ? en.getFullName()    : "";
        String descriptionEn = en != null ? en.getDescription() : "";

        return Category.reconstitute(
            entity.getId(),
            entity.getName(),
            fullName,
            fullNameEn,
            description,
            descriptionEn,
            entity.getImage(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static CategoryTranslationJpaEntity findLocale(CategoryJpaEntity entity, String locale) {
        return entity.getTranslations().stream()
            .filter(t -> locale.equals(t.getLocale()))
            .findFirst()
            .orElse(null);
    }

    private static String blankOr(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private static boolean hasEnContent(Category c) {
        return (c.getFullNameEn()    != null && !c.getFullNameEn().isBlank())
            || (c.getDescriptionEn() != null && !c.getDescriptionEn().isBlank());
    }
}
