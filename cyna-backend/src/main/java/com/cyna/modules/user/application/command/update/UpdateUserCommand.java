package com.cyna.modules.user.application.command.update;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record UpdateUserCommand(
        UUID userId,
        String firstName,
        String lastName,
        String role,
        String status
) implements Command<Void> {}
