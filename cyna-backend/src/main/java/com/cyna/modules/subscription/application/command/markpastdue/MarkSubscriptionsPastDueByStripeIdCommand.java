package com.cyna.modules.subscription.application.command.markpastdue;

import com.cyna.shared.application.Command;

public record MarkSubscriptionsPastDueByStripeIdCommand(
        String stripeSubscriptionId
) implements Command<Void> {}
