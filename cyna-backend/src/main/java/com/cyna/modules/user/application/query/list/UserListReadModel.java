package com.cyna.modules.user.application.query.list;

import java.time.Instant;
import java.util.UUID;

public record UserListReadModel(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        String status,
        Instant createdAt
) {}
