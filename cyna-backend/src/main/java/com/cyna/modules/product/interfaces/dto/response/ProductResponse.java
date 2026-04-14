package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String category,
        String priority,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        String currency,
        String status
) {
    public static ProductResponse from(ProductReadModel model) {
        return new ProductResponse(
                model.id(),
                model.name(),
                model.category(),
                model.priority(),
                model.monthlyPrice(),
                model.annualPrice(),
                model.currency(),
                model.status()
        );
    }
}
