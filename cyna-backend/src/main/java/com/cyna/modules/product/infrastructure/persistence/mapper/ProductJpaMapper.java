package com.cyna.modules.product.infrastructure.persistence.mapper;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.infrastructure.persistence.entity.CategoryJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.cyna.modules.product.infrastructure.persistence.entity.ProductTranslationJpaEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
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

        // Build translation rows
        Set<ProductTranslationJpaEntity> translations = new HashSet<>();

        translations.add(ProductTranslationJpaEntity.of(
            entity,
            "fr",
            product.getName(),
            product.getServiceDescription(),
            product.getTechnicalDescription(),
            product.getHighlightPoints()
        ));

        // EN row is persisted only when at least one EN field carries actual content
        if (hasEnContent(product)) {
            translations.add(ProductTranslationJpaEntity.of(
                entity,
                "en",
                blankOr(product.getNameEn(),               product.getName()),
                blankOr(product.getServiceDescriptionEn(), product.getServiceDescription()),
                blankOr(product.getTechnicalDescriptionEn(), product.getTechnicalDescription()),
                product.getHighlightPointsEn().isEmpty()
                    ? product.getHighlightPoints()
                    : product.getHighlightPointsEn()
            ));
        }

        entity.setTranslations(translations);
        return entity;
    }

    // ── JPA → Domain ─────────────────────────────────────────────────────────

    public Product toDomain(ProductJpaEntity entity) {
        // Extract FR (mandatory) and EN (optional) translations
        ProductTranslationJpaEntity fr = findLocale(entity, "fr");
        ProductTranslationJpaEntity en = findLocale(entity, "en");

        String name                  = fr != null ? fr.getName()                 : "";
        String serviceDescription    = fr != null ? fr.getServiceDescription()   : "";
        String technicalDescription  = fr != null ? fr.getTechnicalDescription() : "";
        List<String> highlightPoints = fr != null ? fr.getHighlightPoints()      : List.of();

        String nameEn                    = en != null ? en.getName()                 : "";
        String serviceDescriptionEn      = en != null ? en.getServiceDescription()   : "";
        String technicalDescriptionEn    = en != null ? en.getTechnicalDescription() : "";
        List<String> highlightPointsEn   = en != null ? en.getHighlightPoints()      : List.of();

        return Product.reconstitute(
            entity.getId(),
            name,
            nameEn,
            entity.getCategory().getId(),
            entity.getPriorityLevel(),
            serviceDescription,
            serviceDescriptionEn,
            technicalDescription,
            technicalDescriptionEn,
            entity.getMonthlyPrice(),
            entity.getAnnualPrice(),
            entity.getCurrency(),
            entity.isPublished(),
            entity.isAvailable(),
            entity.getFreeTrialDays(),
            highlightPoints,
            highlightPointsEn,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ProductTranslationJpaEntity findLocale(ProductJpaEntity entity, String locale) {
        return entity.getTranslations().stream()
            .filter(t -> locale.equals(t.getLocale()))
            .findFirst()
            .orElse(null);
    }

    /** Returns {@code value} if non-blank, otherwise {@code fallback}. */
    private static String blankOr(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    /** True when the product has at least one non-empty EN translatable field. */
    private static boolean hasEnContent(Product p) {
        return (p.getNameEn()               != null && !p.getNameEn().isBlank())
            || (p.getServiceDescriptionEn() != null && !p.getServiceDescriptionEn().isBlank())
            || (p.getTechnicalDescriptionEn() != null && !p.getTechnicalDescriptionEn().isBlank())
            || !p.getHighlightPointsEn().isEmpty();
    }
}
