package com.cyna.modules.subscription.application.command.cancel;

import com.cyna.shared.application.Command;

public record CancelSubscriptionsByStripeIdCommand(
        String stripeSubscriptionId
) implements Command<Void> {}
