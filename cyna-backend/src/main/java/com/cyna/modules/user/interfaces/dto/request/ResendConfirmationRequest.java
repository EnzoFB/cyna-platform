package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.validation.NoHtml;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendConfirmationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @NoHtml(message = "Email must not contain HTML")
        String email,

        @NoHtml(message = "Language must not contain HTML")
        String lang
) {}
