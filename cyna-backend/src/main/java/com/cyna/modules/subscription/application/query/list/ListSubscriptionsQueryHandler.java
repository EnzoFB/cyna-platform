package com.cyna.modules.subscription.application.query.list;

import com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel;
import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import com.cyna.shared.application.QueryHandler;
import com.cyna.shared.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ListSubscriptionsQueryHandler implements QueryHandler<ListSubscriptionsQuery, Page<SubscriptionReadModel>> {

    private final SubscriptionRepository subscriptionRepository;

    public ListSubscriptionsQueryHandler(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Page<SubscriptionReadModel> handle(ListSubscriptionsQuery query) {
        int safePage = Math.max(0, query.page());
        int safeSize = Math.min(Math.max(1, query.size()), 100);

        Page<com.cyna.modules.subscription.domain.model.Subscription> page = subscriptionRepository.findAllByUserId(
                query.userId(),
                safePage,
                safeSize,
                query.sort()
        );

        var items = page.items().stream()
                .map(SubscriptionReadModel::from)
                .toList();

        return new Page<>(items, page.pageNumber(), page.pageSize(), page.totalElements(), page.totalPages());
    }
}
