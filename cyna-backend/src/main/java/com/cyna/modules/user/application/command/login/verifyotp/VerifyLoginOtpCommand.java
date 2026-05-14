package com.cyna.modules.user.application.command.login.verifyotp;

import com.cyna.modules.user.application.model.VerifyOtpOutcome;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record VerifyLoginOtpCommand(
        UUID challengeId,
        String otpCode,
        String userAgent
) implements Command<VerifyOtpOutcome> {

    public VerifyLoginOtpCommand(UUID challengeId, String otpCode) {
        this(challengeId, otpCode, null);
    }
}
