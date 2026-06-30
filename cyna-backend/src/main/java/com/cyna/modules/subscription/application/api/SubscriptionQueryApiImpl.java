package com.cyna.modules.subscription.application.api;

import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class SubscriptionQueryApiImpl implements SubscriptionQueryApi {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionReportingPort reportingPort;

    SubscriptionQueryApiImpl(SubscriptionRepository subscriptionRepository,
                             SubscriptionReportingPort reportingPort) {
        this.subscriptionRepository = subscriptionRepository;
        this.reportingPort = reportingPort;
    }

    @Override
    public long countActiveSubscriptionsAt(Instant atInclusive) {
        return reportingPort.countActiveSubscriptionsAt(atInclusive);
    }

    @Override
    public List<Integer> findSubscriptionYears() {
        return reportingPort.findSubscriptionYears();
    }

    @Override
    public Optional<SubscriptionNotificationView> findForNotification(UUID subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .map(s -> new SubscriptionNotificationView(
                        s.getId(),
                        s.getUserId(),
                        s.getProductName()
                ));
    }

    @Override
    public List<String> findStripeSubscriptionIdsForOrder(UUID userId, UUID orderId) {
        return subscriptionRepository.findByUserIdAndOrderId(userId, orderId).stream()
                .map(com.cyna.modules.subscription.domain.model.Subscription::getStripeSubscriptionId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
    }

    @Override
    public boolean hasEverSubscribed(UUID userId, UUID productId) {
        return subscriptionRepository.existsByUserIdAndProductId(userId, productId);
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
