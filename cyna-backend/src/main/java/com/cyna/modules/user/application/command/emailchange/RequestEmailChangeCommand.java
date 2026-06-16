package com.cyna.modules.user.application.command.emailchange;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record RequestEmailChangeCommand(
        UUID userId,
        String newEmail,
        String lang
) implements Command<Void> {}
