package com.cyna.modules.product.application.query.getcategorybyid;

import com.cyna.modules.product.domain.model.CategoryTranslation;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CategoryReadModel(
        UUID id,
        String name,
        Map<String, CategoryTranslation> translations,
        byte[] image,
        boolean active,
        long productCount,
        Instant createdAt,
        Instant updatedAt
) {}
