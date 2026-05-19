package com.cyna.modules.product.application.query.listpromotions;

import java.math.BigDecimal;
import java.time.Instant;
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
        String marketingTextFr,
        String marketingTextEn,
        Instant startAt,
        Instant endAt,
        boolean enabled,
        boolean activeNow,
        Instant createdAt,
        Instant updatedAt
) {
}

