package com.cyna.modules.product.application.translation;

import com.cyna.modules.product.domain.model.CarouselSettingsTranslation;
import com.cyna.shared.validation.NoHtml;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Application-layer translation payload for the offers carousel. See
 * {@link ProductTranslationDto} for the rationale.
 */
public record CarouselSettingsTranslationDto(
        @NoHtml(message = "Fixed text must not contain HTML")
        String fixedText
) {
    public CarouselSettingsTranslation toDomain() {
        return new CarouselSettingsTranslation(fixedText);
    }

    public static CarouselSettingsTranslationDto fromDomain(CarouselSettingsTranslation t) {
        return new CarouselSettingsTranslationDto(t.fixedText());
    }

    public static Map<String, CarouselSettingsTranslation> toDomainMap(Map<String, CarouselSettingsTranslationDto> dtos) {
        if (dtos == null) {
            return Map.of();
        }
        Map<String, CarouselSettingsTranslation> result = new LinkedHashMap<>();
        dtos.forEach((locale, dto) -> result.put(locale, dto.toDomain()));
        return result;
    }

    public static Map<String, CarouselSettingsTranslationDto> fromDomainMap(Map<String, CarouselSettingsTranslation> domain) {
        if (domain == null) {
            return Map.of();
        }
        Map<String, CarouselSettingsTranslationDto> result = new LinkedHashMap<>();
        domain.forEach((locale, t) -> result.put(locale, fromDomain(t)));
        return result;
    }
}
