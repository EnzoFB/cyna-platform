package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.validation.NoHtml;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @NoHtml(message = "Email must not contain HTML")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @NoHtml(message = "Password must not contain HTML")
        String password,

        @NotBlank(message = "First name is required")
        @NoHtml(message = "First name must not contain HTML")
        String firstName,

        @NotBlank(message = "Last name is required")
        @NoHtml(message = "Last name must not contain HTML")
        String lastName,

        @NotBlank(message = "Role is required")
        @NoHtml(message = "Role must not contain HTML")
        String role
) {}
