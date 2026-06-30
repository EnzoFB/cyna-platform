package com.cyna.modules.user.application.command.create;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record CreateAdminUserCommand(
        String email,
        String password,
        String firstName,
        String lastName,
        String role
) implements Command<UUID> {}
