package com.cyna.modules.subscription.application.query.getbyid;

import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class GetSubscriptionByIdQueryHandler implements QueryHandler<GetSubscriptionByIdQuery, SubscriptionReadModel> {

    private final SubscriptionRepository subscriptionRepository;

    public GetSubscriptionByIdQueryHandler(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public SubscriptionReadModel handle(GetSubscriptionByIdQuery query) {
        return subscriptionRepository.findById(query.subscriptionId())
                .map(SubscriptionReadModel::from)
                .orElse(null);
    }
}
