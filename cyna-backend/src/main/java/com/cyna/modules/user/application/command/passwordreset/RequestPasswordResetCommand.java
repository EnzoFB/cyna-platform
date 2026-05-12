package com.cyna.modules.user.application.command.passwordreset;

import com.cyna.shared.application.Command;

public record RequestPasswordResetCommand(
        String email,
        String lang
) implements Command<Void> {}
