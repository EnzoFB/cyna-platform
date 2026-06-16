package com.cyna.modules.product.application.translation;

import com.cyna.modules.product.domain.model.PromotionTranslation;
import com.cyna.shared.validation.NoHtml;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Application-layer translation payload for a promotion. See
 * {@link ProductTranslationDto} for the rationale.
 */
public record PromotionTranslationDto(
        @NoHtml(message = "Marketing text must not contain HTML")
        String marketingText
) {
    public PromotionTranslation toDomain() {
        return new PromotionTranslation(marketingText);
    }

    public static PromotionTranslationDto fromDomain(PromotionTranslation t) {
        return new PromotionTranslationDto(t.marketingText());
    }

    public static Map<String, PromotionTranslation> toDomainMap(Map<String, PromotionTranslationDto> dtos) {
        if (dtos == null) {
            return Map.of();
        }
        Map<String, PromotionTranslation> result = new LinkedHashMap<>();
        dtos.forEach((locale, dto) -> result.put(locale, dto.toDomain()));
        return result;
    }

    public static Map<String, PromotionTranslationDto> fromDomainMap(Map<String, PromotionTranslation> domain) {
        if (domain == null) {
            return Map.of();
        }
        Map<String, PromotionTranslationDto> result = new LinkedHashMap<>();
        domain.forEach((locale, t) -> result.put(locale, fromDomain(t)));
        return result;
    }
}
