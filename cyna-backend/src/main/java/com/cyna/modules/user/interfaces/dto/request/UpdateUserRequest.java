package com.cyna.modules.user.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Role is required")
        String role,

        @NotBlank(message = "Status is required")
        String status
) {}
