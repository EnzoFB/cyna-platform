package com.cyna.modules.user.application.command.confirmemail;

import com.cyna.shared.application.Command;

public record ResendEmailVerificationCommand(
        String email,
        String lang
) implements Command<Void> {}
