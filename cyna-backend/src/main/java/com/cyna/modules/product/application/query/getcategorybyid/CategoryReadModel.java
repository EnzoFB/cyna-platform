package com.cyna.modules.product.application.query.getcategorybyid;

import java.time.Instant;
import java.util.UUID;

public record CategoryReadModel(
        UUID id,
        String name,
        String fullName,
        String description,
        byte[] image,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
