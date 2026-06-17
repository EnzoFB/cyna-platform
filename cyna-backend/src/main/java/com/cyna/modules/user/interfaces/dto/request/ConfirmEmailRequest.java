package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.validation.NoHtml;
import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailRequest(
        @NotBlank(message = "Token is required")
        @NoHtml(message = "Token must not contain HTML")
        String token
) {}
