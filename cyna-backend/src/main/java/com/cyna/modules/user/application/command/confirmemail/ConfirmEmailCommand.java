package com.cyna.modules.user.application.command.confirmemail;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.shared.application.Command;

public record ConfirmEmailCommand(
        String token
) implements Command<AuthTokens> {}
