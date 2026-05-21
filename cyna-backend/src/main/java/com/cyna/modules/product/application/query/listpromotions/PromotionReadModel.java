package com.cyna.modules.product.application.query.listpromotions;

import com.cyna.modules.product.domain.model.PromotionTranslation;

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
        Map<String, PromotionTranslation> translations,
        Instant startAt,
        Instant endAt,
        boolean enabled,
        boolean showInCarousel,
        Integer carouselOrder,
        boolean activeNow,
        Instant createdAt,
        Instant updatedAt
) {}
