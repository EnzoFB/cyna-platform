package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.validation.NoHtml;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @NoHtml(message = "Email must not contain HTML")
        String email,

        @NotBlank(message = "Password is required")
        @NoHtml(message = "Password must not contain HTML")
        String password,

        @NoHtml(message = "Language must not contain HTML")
        String lang
) {}
