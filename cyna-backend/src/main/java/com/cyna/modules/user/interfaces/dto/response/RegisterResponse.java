package com.cyna.modules.user.interfaces.dto.response;

import com.cyna.modules.user.application.model.RegistrationResult;

/**
 * Returned by POST /auth/register. The account is created pending email
 * verification — no tokens are issued. The frontend prompts the user to check
 * their inbox and confirm.
 */
public record RegisterResponse(
        String status,
        String email
) {
    public static RegisterResponse from(RegistrationResult result) {
        return new RegisterResponse(result.status(), result.email());
    }
}
