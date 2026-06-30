package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.validation.NoHtml;
import com.cyna.shared.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        @NoHtml(message = "Current password must not contain HTML")
        String currentPassword,
        @NotBlank(message = "New password is required")
        @StrongPassword
        @NoHtml(message = "New password must not contain HTML")
        String newPassword
) {}
