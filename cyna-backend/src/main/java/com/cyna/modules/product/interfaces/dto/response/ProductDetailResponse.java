package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;
import com.cyna.modules.product.application.translation.ProductTranslationDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id,
        Map<String, ProductTranslationDto> translations,
        UUID categoryId,
        String categoryName,
        int priorityLevel,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        BigDecimal discountedMonthlyPrice,
        BigDecimal discountedAnnualPrice,
        Integer promotionDiscountPercent,
        String currency,
        boolean isPublished,
        boolean isAvailable,
        int freeTrialDays,
        List<ProductImageData> images,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductDetailResponse from(ProductReadModel model) {
        return new ProductDetailResponse(
                model.id(),
                model.translations(),
                model.categoryId(),
                model.categoryName(),
                model.priorityLevel(),
                model.monthlyPrice(),
                model.annualPrice(),
                model.discountedMonthlyPrice(),
                model.discountedAnnualPrice(),
                model.promotionDiscountPercent(),
                model.currency(),
                model.isPublished(),
                model.isAvailable(),
                model.freeTrialDays(),
                model.images().stream()
                        .map(img -> new ProductImageData(img.id(), img.base64()))
                        .toList(),
                model.createdAt(),
                model.updatedAt()
        );
    }
}
