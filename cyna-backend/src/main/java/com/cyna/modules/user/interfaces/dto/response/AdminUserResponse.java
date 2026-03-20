package com.cyna.modules.user.interfaces.dto.response;

import com.cyna.modules.user.application.query.list.UserListReadModel;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        String status,
        Instant createdAt
) {
    public static AdminUserResponse from(UserListReadModel model) {
        return new AdminUserResponse(
                model.id(),
                model.email(),
                model.firstName(),
                model.lastName(),
                model.role(),
                model.status(),
                model.createdAt()
        );
    }
}
