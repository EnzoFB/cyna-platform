package com.cyna.modules.product.application.query.getbyid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductReadModel(
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
        List<String> imageUrls,
        Instant createdAt,
        Instant updatedAt
) {}
