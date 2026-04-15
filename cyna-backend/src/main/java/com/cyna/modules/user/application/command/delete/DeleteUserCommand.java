package com.cyna.modules.user.application.command.delete;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record DeleteUserCommand(
        UUID userId
) implements Command<Void> {}
