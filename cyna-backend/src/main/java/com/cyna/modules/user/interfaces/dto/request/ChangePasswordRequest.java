package com.cyna.modules.user.interfaces.dto.request;

import com.cyna.shared.validation.NoHtml;

public record ChangePasswordRequest(
        @NoHtml(message = "Current password must not contain HTML")
        String currentPassword,
        @NoHtml(message = "New password must not contain HTML")
        String newPassword
) {}
