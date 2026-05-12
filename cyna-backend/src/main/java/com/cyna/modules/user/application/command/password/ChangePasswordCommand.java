package com.cyna.modules.user.application.command.password;

import com.cyna.modules.user.application.model.AuthTokens;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record ChangePasswordCommand(
        UUID userId,
        String currentPassword,
        String newPassword
) implements Command<AuthTokens> {}
