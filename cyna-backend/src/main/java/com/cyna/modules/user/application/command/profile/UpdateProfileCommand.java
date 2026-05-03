package com.cyna.modules.user.application.command.profile;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record UpdateProfileCommand(
        UUID userId,
        String firstName,
        String lastName,
        String company
) implements Command<Void> {}
