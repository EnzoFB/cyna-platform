package com.cyna.modules.user.application.command.logout;

import com.cyna.shared.application.Command;

public record LogoutCommand(
        String refreshToken,
        boolean allDevices
) implements Command<Void> {

    public LogoutCommand(String refreshToken) {
        this(refreshToken, false);
    }
}
