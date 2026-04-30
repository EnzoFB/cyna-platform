package com.cyna.modules.user.application.command.login.verifyotp;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record VerifyLoginOtpCommand(
        UUID challengeId,
        String otpCode
) implements Command<AuthTokens> {}
