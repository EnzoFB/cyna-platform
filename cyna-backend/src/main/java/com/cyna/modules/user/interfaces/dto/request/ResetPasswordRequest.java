package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.validation.NoHtml;
import com.cyna.shared.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "Token is required")
        @NoHtml(message = "Token must not contain HTML")
        String token,

        @NotBlank(message = "Password is required")
        @StrongPassword
        @NoHtml(message = "Password must not contain HTML")
        String newPassword
) {}
