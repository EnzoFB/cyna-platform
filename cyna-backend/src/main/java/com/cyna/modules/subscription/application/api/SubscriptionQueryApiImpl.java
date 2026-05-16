package com.cyna.modules.subscription.application.api;

import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
class SubscriptionQueryApiImpl implements SubscriptionQueryApi {

    private final SubscriptionRepository subscriptionRepository;

    SubscriptionQueryApiImpl(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public List<SubscriptionExportView> exportForUser(UUID userId) {
        return subscriptionRepository.findByUserId(userId).stream()
                .map(s -> new SubscriptionExportView(
                        s.getId(),
                        s.getProductName(),
                        s.getProductCategory(),
                        s.getBillingCycle().name(),
                        s.getStatus().name(),
                        s.getQuantity(),
                        s.getUnitPrice().amount(),
                        s.getUnitPrice().currency(),
                        s.getStartAt(),
                        s.getEndAt(),
                        s.getNextBillingAt(),
                        s.getCancelledAt(),
                        s.isAutoRenew()
                ))
                .toList();
    }
}
