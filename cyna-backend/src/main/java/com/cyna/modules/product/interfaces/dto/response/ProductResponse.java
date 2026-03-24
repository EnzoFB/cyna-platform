package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.getbyid.ProductReadModel;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String category,
        String priority,
        String serviceDescription,
        String technicalDescription,
        PriceResponse monthlyPrice,
        PriceResponse annualPrice,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(ProductReadModel model) {
        return new ProductResponse(
                model.id(),
                model.name(),
                model.category(),
                model.priority(),
                model.serviceDescription(),
                model.technicalDescription(),
                new PriceResponse(model.monthlyPrice(), model.currency()),
                new PriceResponse(model.annualPrice(), model.currency()),
                model.status(),
                model.createdAt(),
                model.updatedAt()
        );
    }
}
