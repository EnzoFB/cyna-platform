package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.getcategorybyid.CategoryReadModel;
import com.cyna.modules.product.application.translation.CategoryTranslationDto;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        Map<String, CategoryTranslationDto> translations,
        String imageBase64,
        boolean active,
        long productCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static CategoryResponse from(CategoryReadModel model) {
        String imageBase64 = model.image() != null
                ? Base64.getEncoder().encodeToString(model.image())
                : null;

        return new CategoryResponse(
                model.id(),
                model.name(),
                model.translations(),
                imageBase64,
                model.active(),
                model.productCount(),
                model.createdAt(),
                model.updatedAt()
        );
    }
}
