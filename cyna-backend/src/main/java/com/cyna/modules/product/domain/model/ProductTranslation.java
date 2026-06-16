package com.cyna.modules.product.domain.model;

import java.util.List;

/**
 * Domain value object — a product's localized text. Pure Java: input
 * sanitization (@NoHtml) lives on the application-layer translation payload,
 * not on the domain record.
 */
public record ProductTranslation(
        String name,
        String serviceDescription,
        String technicalDescription,
        List<String> highlightPoints
) {
    public ProductTranslation {
        highlightPoints = highlightPoints != null ? List.copyOf(highlightPoints) : List.of();
    }
}
