package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        UUID categoryId,
        String categoryName,
        int priorityLevel,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        BigDecimal discountedMonthlyPrice,
        BigDecimal discountedAnnualPrice,
        Integer promotionDiscountPercent,
        String currency,
        String primaryImageBase64,
        boolean isPublished,
        boolean isAvailable
) {
    public static ProductResponse from(ProductReadModel model) {
        String primaryImage = model.images().isEmpty() ? null : model.images().getFirst().base64();
        return new ProductResponse(
                model.id(),
                model.name(),
                model.categoryId(),
                model.categoryName(),
                model.priorityLevel(),
                model.monthlyPrice(),
                model.annualPrice(),
                model.discountedMonthlyPrice(),
                model.discountedAnnualPrice(),
                model.promotionDiscountPercent(),
                model.currency(),
                primaryImage,
                model.isPublished(),
                model.isAvailable()
        );
    }
}
