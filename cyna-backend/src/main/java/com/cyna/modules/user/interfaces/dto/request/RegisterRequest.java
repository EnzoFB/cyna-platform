package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.interfaces.rest.validation.NoHtml;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "First name is required")
        @NoHtml(message = "First name must not contain HTML")
        String firstName,

        @NotBlank(message = "Last name is required")
        @NoHtml(message = "Last name must not contain HTML")
        String lastName,

        @NotBlank(message = "Language is required")
        String lang
) {}
