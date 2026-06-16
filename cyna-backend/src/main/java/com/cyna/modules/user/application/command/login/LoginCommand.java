package com.cyna.modules.user.application.command.login;

import com.cyna.modules.user.application.model.LoginOutcome;
import com.cyna.shared.application.Command;

public record LoginCommand(
        String email,
        String password,
        String requiredRole,
        String lang,
        String trustedDeviceToken,
        String userAgent
) implements Command<LoginOutcome> {

    public LoginCommand(String email, String password) {
        this(email, password, null, "fr", null, null);
    }

    public LoginCommand(String email, String password, String requiredRole) {
        this(email, password, requiredRole, "fr", null, null);
    }

    public LoginCommand(String email, String password, String requiredRole, String lang) {
        this(email, password, requiredRole, lang, null, null);
    }
}
