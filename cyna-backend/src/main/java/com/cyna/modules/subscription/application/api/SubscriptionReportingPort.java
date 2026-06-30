package com.cyna.modules.subscription.application.api;

import java.time.Instant;
import java.util.List;

/**
 * Internal reporting port for subscription analytics. Implemented by an
 * infrastructure adapter that reads {@code subscription_schema} only. Exposed to
 * other modules through {@code SubscriptionQueryApi}.
 */
public interface SubscriptionReportingPort {

    long countActiveSubscriptionsAt(Instant atInclusive);

    List<Integer> findSubscriptionYears();
}
