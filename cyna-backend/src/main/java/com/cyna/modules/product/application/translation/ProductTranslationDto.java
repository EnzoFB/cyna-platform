package com.cyna.modules.product.application.translation;

import com.cyna.modules.product.domain.model.ProductTranslation;
import com.cyna.shared.validation.NoHtml;
import com.cyna.shared.validation.NoHtmlElements;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application-layer translation payload for a product. Carries the anti-XSS
 * constraints so it can be reused by the request/response DTOs (interfaces) and
 * by the commands/read models (application) without leaking the domain value
 * object across the interfaces boundary.
 */
public record ProductTranslationDto(
        @NoHtml(message = "Product name must not contain HTML")
        String name,
        @NoHtml(message = "Service description must not contain HTML")
        String serviceDescription,
        @NoHtml(message = "Technical description must not contain HTML")
        String technicalDescription,
        @NoHtmlElements(message = "Highlight points must not contain HTML")
        List<String> highlightPoints
) {
    public ProductTranslationDto {
        highlightPoints = highlightPoints != null ? List.copyOf(highlightPoints) : List.of();
    }

    public ProductTranslation toDomain() {
        return new ProductTranslation(name, serviceDescription, technicalDescription, highlightPoints);
    }

    public static ProductTranslationDto fromDomain(ProductTranslation t) {
        return new ProductTranslationDto(t.name(), t.serviceDescription(), t.technicalDescription(), t.highlightPoints());
    }

    public static Map<String, ProductTranslation> toDomainMap(Map<String, ProductTranslationDto> dtos) {
        if (dtos == null) {
            return Map.of();
        }
        Map<String, ProductTranslation> result = new LinkedHashMap<>();
        dtos.forEach((locale, dto) -> result.put(locale, dto.toDomain()));
        return result;
    }

    public static Map<String, ProductTranslationDto> fromDomainMap(Map<String, ProductTranslation> domain) {
        if (domain == null) {
            return Map.of();
        }
        Map<String, ProductTranslationDto> result = new LinkedHashMap<>();
        domain.forEach((locale, t) -> result.put(locale, fromDomain(t)));
        return result;
    }
}
