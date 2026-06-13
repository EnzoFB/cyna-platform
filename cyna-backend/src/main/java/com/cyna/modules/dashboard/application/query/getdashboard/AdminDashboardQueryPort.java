package com.cyna.modules.dashboard.application.query.getdashboard;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface AdminDashboardQueryPort {

    long sumRevenueBetween(Instant fromInclusive, Instant toExclusive);

    long sumSalesQuantityBetween(Instant fromInclusive, Instant toExclusive);

    long countCustomersCreatedBetween(Instant fromInclusive, Instant toExclusive);

    long countCustomersCreatedBefore(Instant beforeExclusive);

    long countActiveSubscriptionsAt(Instant atInclusive);

    List<MonthlyRevenueAggregate> findMonthlyRevenueByYear(int year);

    List<TopProductAggregate> findTopProductsByYear(int year, String locale, int limit);

    List<Integer> findAvailableYears();

    /** Returns order counts grouped by status for the given fiscal year. */
    Map<String, Long> countOrdersByStatusForYear(int year);
}
