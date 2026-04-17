package com.cyna.modules.user.application.command.login;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.shared.application.Command;

public record LoginCommand(
        String email,
        String password,
        String requiredRole
) implements Command<AuthTokens> {

    public LoginCommand(String email, String password) {
        this(email, password, null);
    }
}
