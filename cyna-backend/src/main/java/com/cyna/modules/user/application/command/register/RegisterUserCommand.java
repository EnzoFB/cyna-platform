package com.cyna.modules.user.application.command.register;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.shared.application.Command;

public record RegisterUserCommand(
        String email,
        String password,
        String firstName,
        String lastName,
        String company,
        String lang,
        // RGPD Art. 7.1 proof-of-consent context. acceptedTerms must be true;
        // ip/userAgent are captured server-side from the request.
        boolean acceptedTerms,
        String ipAddress,
        String userAgent
) implements Command<AuthTokens> {}
