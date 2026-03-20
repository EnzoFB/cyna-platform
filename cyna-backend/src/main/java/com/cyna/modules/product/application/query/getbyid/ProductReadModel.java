package com.cyna.modules.product.application.query.getbyid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductReadModel(
        UUID id,
        String name,
        String category,
        String priority,
        String serviceDescription,
        String technicalDescription,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
