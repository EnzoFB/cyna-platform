package com.cyna.modules.product.domain.model;

/**
 * Domain value object — a promotion's localized marketing text. Pure Java:
 * input sanitization lives on the application-layer translation payload.
 */
public record PromotionTranslation(
        String marketingText
) {}
