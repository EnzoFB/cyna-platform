package com.cyna.modules.payment.application.command.savedpaymentmethod;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record CreateSetupIntentCommand(UUID userId) implements Command<String> {}
