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
        String currency,
        String primaryImageUrl,
        boolean isPublished,
        boolean isAvailable
) {
    public static ProductResponse from(ProductReadModel model) {
        return new ProductResponse(
                model.id(),
                model.name(),
                model.categoryId(),
                model.categoryName(),
                model.priorityLevel(),
                model.monthlyPrice(),
                model.annualPrice(),
                model.currency(),
                model.imageUrls().isEmpty() ? null : model.imageUrls().getFirst(),
                model.isPublished(),
                model.isAvailable()
        );
    }
}
