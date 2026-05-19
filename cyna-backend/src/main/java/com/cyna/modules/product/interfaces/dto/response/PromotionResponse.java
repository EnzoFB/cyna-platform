package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.listpromotions.PromotionReadModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PromotionResponse(
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
    public static PromotionResponse from(PromotionReadModel model) {
        return new PromotionResponse(
                model.id(),
                model.productId(),
                model.productName(),
                model.productCategoryName(),
                model.baseMonthlyPrice(),
                model.baseAnnualPrice(),
                model.discountedMonthlyPrice(),
                model.discountedAnnualPrice(),
                model.currency(),
                model.discountPercent(),
                model.marketingTextFr(),
                model.marketingTextEn(),
                model.startAt(),
                model.endAt(),
                model.enabled(),
                model.activeNow(),
                model.createdAt(),
                model.updatedAt()
        );
    }
}

