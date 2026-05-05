package com.cyna.modules.subscription.application.command.autorenew;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record UpdateSubscriptionAutoRenewCommand(
        UUID subscriptionId,
        UUID userId,
        boolean autoRenew
) implements Command<SubscriptionReadModel> {
}
