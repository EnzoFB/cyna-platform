package com.cyna.modules.product.application.query.getbyid;

import com.cyna.modules.product.domain.model.ProductTranslation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductReadModel(
        UUID id,
        Map<String, ProductTranslation> translations,
        UUID categoryId,
        String categoryName,
        int priorityLevel,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        BigDecimal discountedMonthlyPrice,
        BigDecimal discountedAnnualPrice,
        Integer promotionDiscountPercent,
        Instant promotionStartAt,
        Instant promotionEndAt,
        String currency,
        boolean isPublished,
        boolean isAvailable,
        int freeTrialDays,
        List<ProductImageReadModel> images,
        Instant createdAt,
        Instant updatedAt
) {}
