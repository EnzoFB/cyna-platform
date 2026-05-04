package com.cyna.modules.payment.application.command.processwebhook;

import com.cyna.shared.application.Command;

public record ProcessWebhookCommand(
        String payload,
        String sigHeader
) implements Command<Void> {}
