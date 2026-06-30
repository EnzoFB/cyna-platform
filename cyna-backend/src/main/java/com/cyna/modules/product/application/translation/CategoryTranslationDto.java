package com.cyna.modules.product.application.translation;

import com.cyna.modules.product.domain.model.CategoryTranslation;
import com.cyna.shared.validation.NoHtml;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Application-layer translation payload for a category. See
 * {@link ProductTranslationDto} for the rationale.
 */
public record CategoryTranslationDto(
        @NoHtml(message = "Category full name must not contain HTML")
        String fullName,
        @NoHtml(message = "Category description must not contain HTML")
        String description
) {
    public CategoryTranslation toDomain() {
        return new CategoryTranslation(fullName, description);
    }

    public static CategoryTranslationDto fromDomain(CategoryTranslation t) {
        return new CategoryTranslationDto(t.fullName(), t.description());
    }

    public static Map<String, CategoryTranslation> toDomainMap(Map<String, CategoryTranslationDto> dtos) {
        if (dtos == null) {
            return Map.of();
        }
        Map<String, CategoryTranslation> result = new LinkedHashMap<>();
        dtos.forEach((locale, dto) -> result.put(locale, dto.toDomain()));
        return result;
    }

    public static Map<String, CategoryTranslationDto> fromDomainMap(Map<String, CategoryTranslation> domain) {
        if (domain == null) {
            return Map.of();
        }
        Map<String, CategoryTranslationDto> result = new LinkedHashMap<>();
        domain.forEach((locale, t) -> result.put(locale, fromDomain(t)));
        return result;
    }
}
