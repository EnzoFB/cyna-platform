package com.cyna.modules.subscription.application.query.getbyid;

import com.cyna.shared.application.Query;

import java.util.UUID;

public record GetSubscriptionByIdQuery(UUID subscriptionId) implements Query<SubscriptionReadModel> {
}
