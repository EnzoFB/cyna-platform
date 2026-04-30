package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.getcategorybyid.CategoryReadModel;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String fullName,
        String description,
        String imageBase64,
        boolean active,
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
                model.fullName(),
                model.description(),
                imageBase64,
                model.active(),
                model.createdAt(),
                model.updatedAt()
        );
    }
}
