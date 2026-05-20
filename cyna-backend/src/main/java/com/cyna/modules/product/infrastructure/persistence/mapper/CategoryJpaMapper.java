package com.cyna.modules.product.infrastructure.persistence.mapper;

import com.cyna.modules.product.domain.model.Category;
import com.cyna.modules.product.domain.model.CategoryTranslation;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryTranslationJpaEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

        Set<CategoryTranslationJpaEntity> translations = new HashSet<>();
        category.getTranslations().forEach((locale, t) ->
                translations.add(CategoryTranslationJpaEntity.of(entity, locale, t.fullName(), t.description()))
        );
        entity.setTranslations(translations);
        return entity;
    }

    // ── JPA → Domain ─────────────────────────────────────────────────────────

    public Category toDomain(CategoryJpaEntity entity) {
        Map<String, CategoryTranslation> translations = new HashMap<>();
        entity.getTranslations().forEach(t ->
                translations.put(t.getLocale(), new CategoryTranslation(t.getFullName(), t.getDescription()))
        );

        // Ensure at least a "fr" entry to satisfy domain validation
        if (!translations.containsKey("fr")) {
            translations.put("fr", new CategoryTranslation("", ""));
        }

        return Category.reconstitute(
                entity.getId(),
                entity.getName(),
                translations,
                entity.getImage(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
