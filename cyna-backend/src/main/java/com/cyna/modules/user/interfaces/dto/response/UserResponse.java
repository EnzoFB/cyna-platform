package com.cyna.modules.user.interfaces.dto.response;

import com.cyna.modules.user.application.query.me.UserReadModel;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        Instant createdAt
) {
    public static UserResponse from(UserReadModel model) {
        return new UserResponse(
                model.id(),
                model.email(),
                model.firstName(),
                model.lastName(),
                model.role(),
                model.createdAt()
        );
    }
}
