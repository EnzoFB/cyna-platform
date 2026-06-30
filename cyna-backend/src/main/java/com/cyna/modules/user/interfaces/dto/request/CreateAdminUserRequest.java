package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.validation.NoHtml;
import com.cyna.shared.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateAdminUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @NoHtml(message = "Email must not contain HTML")
        String email,

        @NotBlank(message = "Password is required")
        @StrongPassword
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
