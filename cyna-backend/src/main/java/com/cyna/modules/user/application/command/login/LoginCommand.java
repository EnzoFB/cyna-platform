package com.cyna.modules.user.application.command.login;

import com.cyna.modules.user.application.model.LoginChallenge;
import com.cyna.shared.application.Command;

public record LoginCommand(
        String email,
        String password
) implements Command<LoginChallenge> {}
