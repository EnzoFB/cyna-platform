package com.cyna.modules.product.infrastructure.persistence.mapper;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.ProductTranslation;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductTranslationJpaEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class ProductJpaMapper {

    // ── Domain → JPA ─────────────────────────────────────────────────────────

    public ProductJpaEntity toJpa(Product product) {
        var entity = new ProductJpaEntity();
        entity.setId(product.getId());

        var categoryRef = new CategoryJpaEntity();
        categoryRef.setId(product.getCategoryId());
        entity.setCategory(categoryRef);

        entity.setPriorityLevel(product.getPriorityLevel());
        entity.setMonthlyPrice(product.getMonthlyPrice());
        entity.setAnnualPrice(product.getAnnualPrice());
        entity.setCurrency(product.getCurrency());
        entity.setPublished(product.isPublished());
        entity.setAvailable(product.isAvailable());
        entity.setFreeTrialDays(product.getFreeTrialDays());
        entity.setCreatedAt(product.getCreatedAt());
        entity.setUpdatedAt(product.getUpdatedAt());

        Set<ProductTranslationJpaEntity> translations = new HashSet<>();
        product.getTranslations().forEach((locale, t) ->
                translations.add(ProductTranslationJpaEntity.of(
                        entity,
                        locale,
                        t.name(),
                        t.serviceDescription(),
                        t.technicalDescription(),
                        t.highlightPoints()
                ))
        );
        entity.setTranslations(translations);
        return entity;
    }

    // ── JPA → Domain ─────────────────────────────────────────────────────────

    public Product toDomain(ProductJpaEntity entity) {
        Map<String, ProductTranslation> translations = new HashMap<>();
        entity.getTranslations().forEach(t ->
                translations.put(t.getLocale(), new ProductTranslation(
                        t.getName(),
                        t.getServiceDescription(),
                        t.getTechnicalDescription(),
                        t.getHighlightPoints()
                ))
        );

        // Ensure at least a "fr" entry to satisfy domain validation
        if (!translations.containsKey("fr")) {
            translations.put("fr", new ProductTranslation("", "", "", java.util.List.of()));
        }

        return Product.reconstitute(
                entity.getId(),
                translations,
                entity.getCategory().getId(),
                entity.getPriorityLevel(),
                entity.getMonthlyPrice(),
                entity.getAnnualPrice(),
                entity.getCurrency(),
                entity.isPublished(),
                entity.isAvailable(),
                entity.getFreeTrialDays(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
