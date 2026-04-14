package com.cyna.modules.subscription.application.command.cancel;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record CancelSubscriptionCommand(
        UUID subscriptionId,
        UUID userId
) implements Command<SubscriptionReadModel> {
}
