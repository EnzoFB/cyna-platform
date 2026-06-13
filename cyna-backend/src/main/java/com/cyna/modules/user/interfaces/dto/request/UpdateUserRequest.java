package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.interfaces.rest.validation.NoHtml;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank(message = "First name is required")
        @NoHtml(message = "First name must not contain HTML")
        String firstName,

        @NotBlank(message = "Last name is required")
        @NoHtml(message = "Last name must not contain HTML")
        String lastName,

        @NotBlank(message = "Role is required")
        @NoHtml(message = "Role must not contain HTML")
        String role,

        @NotBlank(message = "Status is required")
        @NoHtml(message = "Status must not contain HTML")
        String status
) {}
