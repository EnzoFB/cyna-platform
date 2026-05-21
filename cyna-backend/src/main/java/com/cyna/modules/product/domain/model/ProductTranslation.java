package com.cyna.modules.product.domain.model;

import java.util.List;

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
