package com.cyna.shared.interfaces.dto.request;

import com.cyna.shared.validation.NoHtml;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        @NoHtml(message = "Name must not contain HTML")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @NoHtml(message = "Email must not contain HTML")
        String email,

        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject must not exceed 200 characters")
        @NoHtml(message = "Subject must not contain HTML")
        String subject,

        @NotBlank(message = "Message is required")
        @Size(max = 5000, message = "Message must not exceed 5000 characters")
        @NoHtml(message = "Message must not contain HTML")
        String message,

        @NoHtml(message = "Language must not contain HTML")
        String lang
) {}
