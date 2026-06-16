package com.cyna.modules.product.domain.model;

/**
 * Domain value object — the carousel's localized fixed text. Pure Java: input
 * sanitization lives on the application-layer translation payload.
 */
public record CarouselSettingsTranslation(
        String fixedText
) {}
