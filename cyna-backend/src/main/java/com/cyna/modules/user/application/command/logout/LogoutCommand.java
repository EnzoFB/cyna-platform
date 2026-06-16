package com.cyna.modules.user.application.command.logout;

import com.cyna.shared.application.Command;

public record LogoutCommand(String refreshToken) implements Command<Void> {
}
