package com.cyna.modules.user.application.query.me;

import java.time.Instant;
import java.util.UUID;

public record UserReadModel(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String company,
        String role,
        Instant createdAt
) {}
