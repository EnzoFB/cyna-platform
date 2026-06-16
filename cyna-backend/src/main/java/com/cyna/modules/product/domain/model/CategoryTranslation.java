package com.cyna.modules.product.domain.model;

/**
 * Domain value object — a category's localized text. Pure Java: input
 * sanitization lives on the application-layer translation payload.
 */
public record CategoryTranslation(
        String fullName,
        String description
) {}
