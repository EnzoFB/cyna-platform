package com.cyna.modules.user.application.command.passwordreset;

import com.cyna.shared.application.Command;

public record ResetPasswordCommand(
        String token,
        String newPassword
) implements Command<Void> {}
