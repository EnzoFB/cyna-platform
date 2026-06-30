package com.cyna.modules.product.application.query.listpromotions;

import com.cyna.modules.product.application.translation.PromotionTranslationDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PromotionReadModel(
        UUID id,
        UUID productId,
        String productName,
        String productCategoryName,
        BigDecimal baseMonthlyPrice,
        BigDecimal baseAnnualPrice,
        BigDecimal discountedMonthlyPrice,
        BigDecimal discountedAnnualPrice,
        String currency,
        int discountPercent,
        Map<String, PromotionTranslationDto> translations,
        Instant startAt,
        Instant endAt,
        boolean enabled,
        boolean showInCarousel,
        Integer carouselOrder,
        boolean activeNow,
        boolean productAvailable,
        boolean productPublished,
        Instant createdAt,
        Instant updatedAt
) {}
