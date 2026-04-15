package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
    UUID id,
    String name,
    UUID categoryId,
    String categoryName,
    int priorityLevel,
    String serviceDescription,
    String technicalDescription,
    BigDecimal monthlyPrice,
    BigDecimal annualPrice,
    String currency,
    boolean isPublished,
    boolean isAvailable,
    int freeTrialDays,
    List<String> highlightPoints,
    Instant createdAt,
    Instant updatedAt
) {
    public static ProductDetailResponse from(ProductReadModel model) {
        return new ProductDetailResponse(
            model.id(),
            model.name(),
            model.categoryId(),
            model.categoryName(),
            model.priorityLevel(),
            model.serviceDescription(),
            model.technicalDescription(),
            model.monthlyPrice(),
            model.annualPrice(),
            model.currency(),
            model.isPublished(),
            model.isAvailable(),
            model.freeTrialDays(),
            model.highlightPoints(),
            model.createdAt(),
            model.updatedAt()
        );
    }
}
