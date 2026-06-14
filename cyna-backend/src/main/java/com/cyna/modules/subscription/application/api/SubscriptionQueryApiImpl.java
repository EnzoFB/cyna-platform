package com.cyna.modules.subscription.application.api;

import com.cyna.modules.subscription.domain.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
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
