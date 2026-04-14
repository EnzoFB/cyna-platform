package com.cyna.modules.subscription.domain.repository;

import com.cyna.modules.subscription.application.query.list.SubscriptionSort;
import com.cyna.modules.subscription.domain.model.Subscription;
import com.cyna.shared.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {
    void save(Subscription subscription);
    Optional<Subscription> findById(UUID id);
    Page<Subscription> findAllByUserId(UUID userId, int page, int size, SubscriptionSort sort);
}
