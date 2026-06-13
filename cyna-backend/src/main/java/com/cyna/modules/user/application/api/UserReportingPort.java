package com.cyna.modules.user.application.api;

import java.time.Instant;
import java.util.List;

/**
 * Internal reporting port for customer analytics. Implemented by an
 * infrastructure adapter that reads {@code user_schema} only. Exposed to other
 * modules through {@code UserQueryApi}.
 */
public interface UserReportingPort {

    long countCustomersCreatedBetween(Instant fromInclusive, Instant toExclusive);

    long countCustomersCreatedBefore(Instant beforeExclusive);

    List<Integer> findCustomerYears();
}
