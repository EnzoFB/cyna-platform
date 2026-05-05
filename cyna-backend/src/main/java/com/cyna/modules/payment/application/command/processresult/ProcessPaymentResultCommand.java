package com.cyna.modules.payment.application.command.processresult;

import com.cyna.shared.application.Command;

public record ProcessPaymentResultCommand(
        String stripePaymentIntentId,
        boolean succeeded
) implements Command<Void> {}
