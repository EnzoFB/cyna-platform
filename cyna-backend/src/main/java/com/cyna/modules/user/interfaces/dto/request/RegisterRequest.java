package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.interfaces.rest.validation.NoHtml;
import jakarta.validation.constraints.AssertTrue;
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
        String lang,

        // RGPD Art. 7 — explicit, mandatory acceptance of the Terms of Service
        // and Privacy Policy. Must be true; the consent wording version is
        // stamped server-side so the client cannot forge it.
        @AssertTrue(message = "You must accept the Terms of Service and the Privacy Policy")
        boolean acceptTerms
) {}
