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

    /** Sales (revenue + count) for each of the last {@code days} calendar days, oldest first. */
    List<DailyRevenueAggregate> findDailyRevenue(int days);

    /** Sales (revenue + count) for each of the last {@code weeks} ISO weeks, oldest first. */
    List<WeeklyRevenueAggregate> findWeeklyRevenue(int weeks);

    /** Average cart value per product category for the given fiscal year. */
    List<CategoryAvgCartAggregate> findCategoryAverageCartByYear(int year);

    /** Average cart value per product category over the given time window. */
    List<CategoryAvgCartAggregate> findCategoryAverageCartBetween(Instant fromInclusive, Instant toExclusive);

    /** Revenue and quantity sold per product category for the given fiscal year. */
    List<CategorySalesAggregate> findCategorySalesByYear(int year);

    /** Revenue and quantity sold per product category over the given time window. */
    List<CategorySalesAggregate> findCategorySalesBetween(Instant fromInclusive, Instant toExclusive);
}
