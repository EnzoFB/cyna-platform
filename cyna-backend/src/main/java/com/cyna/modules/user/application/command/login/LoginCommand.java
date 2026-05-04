package com.cyna.modules.user.application.command.login;

import com.cyna.modules.user.application.model.LoginChallenge;
import com.cyna.shared.application.Command;

public record LoginCommand(
        String email,
        String password,
        String requiredRole,
        String lang
) implements Command<LoginChallenge> {

    public LoginCommand(String email, String password) {
        this(email, password, null, "fr");
    }

    public LoginCommand(String email, String password, String requiredRole) {
        this(email, password, requiredRole, "fr");
    }
}
