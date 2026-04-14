package com.cyna.modules.subscription.application.query.list;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.shared.application.Query;
import com.cyna.shared.domain.Page;

import java.util.UUID;

public record ListSubscriptionsQuery(
        UUID userId,
        int page,
        int size,
        SubscriptionSort sort
) implements Query<Page<SubscriptionReadModel>> {
}
