package com.cyna.modules.user.application.command.register;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.shared.application.Command;

public record RegisterUserCommand(
        String email,
        String password,
        String firstName,
        String lastName,
        String lang
) implements Command<AuthTokens> {}
