package com.cyna.modules.user.application.command.refresh;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.shared.application.Command;

public record RefreshTokenCommand(
        String refreshToken
) implements Command<AuthTokens> {}
