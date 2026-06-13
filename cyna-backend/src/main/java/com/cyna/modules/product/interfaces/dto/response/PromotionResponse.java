package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.listpromotions.PromotionReadModel;
import com.cyna.modules.product.domain.model.PromotionTranslation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
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
        Map<String, PromotionTranslation> translations,
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
                model.translations(),
                model.startAt(),
                model.endAt(),
                model.enabled(),
                model.showInCarousel(),
                model.carouselOrder(),
                model.activeNow(),
                model.productAvailable(),
                model.productPublished(),
                model.createdAt(),
                model.updatedAt()
        );
    }
}
