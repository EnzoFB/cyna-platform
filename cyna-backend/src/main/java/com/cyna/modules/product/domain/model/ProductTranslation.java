package com.cyna.modules.product.domain.model;

import com.cyna.shared.interfaces.rest.validation.NoHtml;
import com.cyna.shared.interfaces.rest.validation.NoHtmlElements;

import java.util.List;

public record ProductTranslation(
        @NoHtml(message = "Product name must not contain HTML")
        String name,
        @NoHtml(message = "Service description must not contain HTML")
        String serviceDescription,
        @NoHtml(message = "Technical description must not contain HTML")
        String technicalDescription,
        @NoHtmlElements(message = "Highlight points must not contain HTML")
        List<String> highlightPoints
) {
    public ProductTranslation {
        highlightPoints = highlightPoints != null ? List.copyOf(highlightPoints) : List.of();
    }
}
