package com.cyna.modules.product.application.query.getcategorybyid;

import com.cyna.modules.product.application.translation.CategoryTranslationDto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CategoryReadModel(
        UUID id,
        String name,
        Map<String, CategoryTranslationDto> translations,
        byte[] image,
        boolean active,
        long productCount,
        Instant createdAt,
        Instant updatedAt
) {}
