package com.cyna.modules.user.application.command.emailchange;

import com.cyna.shared.application.Command;

public record ConfirmEmailChangeCommand(String token) implements Command<Void> {}
