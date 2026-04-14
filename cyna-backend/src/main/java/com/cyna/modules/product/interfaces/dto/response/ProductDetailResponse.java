package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductDetailResponse(
    UUID id,
    String name,
    String category,
    String priority,
    String serviceDescription,
    String technicalDescription,
    BigDecimal monthlyPrice,
    BigDecimal annualPrice,
    String currency,
    String status
) {
    public static ProductDetailResponse from(ProductReadModel model) {
        return new ProductDetailResponse(
            model.id(),
            model.name(),
            model.category(),
            model.priority(),
            model.serviceDescription(),
            model.technicalDescription(),
            model.monthlyPrice(),
            model.annualPrice(),
            model.currency(),
            model.status()
        );
    }
}
