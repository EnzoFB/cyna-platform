package com.cyna.modules.product.domain.model;

import com.cyna.shared.interfaces.rest.validation.NoHtml;

public record CategoryTranslation(
        @NoHtml(message = "Category full name must not contain HTML")
        String fullName,
        @NoHtml(message = "Category description must not contain HTML")
        String description
) {}
