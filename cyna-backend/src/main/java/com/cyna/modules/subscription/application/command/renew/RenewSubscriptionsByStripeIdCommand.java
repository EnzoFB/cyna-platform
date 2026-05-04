package com.cyna.modules.subscription.application.command.renew;

import com.cyna.shared.application.Command;

import java.time.Instant;

public record RenewSubscriptionsByStripeIdCommand(
        String stripeSubscriptionId,
        Instant newPeriodEnd
) implements Command<Void> {}
