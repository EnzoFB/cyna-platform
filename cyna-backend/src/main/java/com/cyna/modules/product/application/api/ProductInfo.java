package com.cyna.modules.product.application.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductInfo(
        UUID id,
        String name,
        String serviceDescription,
        String technicalDescription,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        String currency,
        String status,
        String category,
        String priority
) {}
