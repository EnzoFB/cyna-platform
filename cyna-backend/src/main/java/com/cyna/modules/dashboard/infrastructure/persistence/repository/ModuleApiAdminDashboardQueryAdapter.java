package com.cyna.modules.dashboard.infrastructure.persistence.repository;

import com.cyna.modules.dashboard.application.query.getdashboard.AdminDashboardQueryPort;
import com.cyna.modules.dashboard.application.query.getdashboard.CategoryAvgCartAggregate;
import com.cyna.modules.dashboard.application.query.getdashboard.CategorySalesAggregate;
import com.cyna.modules.dashboard.application.query.getdashboard.DailyRevenueAggregate;
import com.cyna.modules.dashboard.application.query.getdashboard.MonthlyRevenueAggregate;
import com.cyna.modules.dashboard.application.query.getdashboard.TopProductAggregate;
import com.cyna.modules.dashboard.application.query.getdashboard.WeeklyRevenueAggregate;
import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.subscription.application.api.SubscriptionQueryApi;
import com.cyna.modules.user.application.api.UserQueryApi;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Assembles the admin dashboard purely by orchestrating the order, user and
 * subscription modules' published reporting APIs. The dashboard module holds no
 * cross-schema SQL: every aggregate is computed by the module that owns the data,
 * over its own schema. At a microservice split these become remote calls — the
 * boundary is already the {@code application.api} seam.
 */
@Repository
public class ModuleApiAdminDashboardQueryAdapter implements AdminDashboardQueryPort {

    private final OrderQueryApi orderQueryApi;
    private final UserQueryApi userQueryApi;
    private final SubscriptionQueryApi subscriptionQueryApi;

    public ModuleApiAdminDashboardQueryAdapter(OrderQueryApi orderQueryApi,
                                               UserQueryApi userQueryApi,
                                               SubscriptionQueryApi subscriptionQueryApi) {
        this.orderQueryApi = orderQueryApi;
        this.userQueryApi = userQueryApi;
        this.subscriptionQueryApi = subscriptionQueryApi;
    }

    @Override
    public long sumRevenueBetween(Instant fromInclusive, Instant toExclusive) {
        return orderQueryApi.sumRevenueBetween(fromInclusive, toExclusive);
    }

    @Override
    public long sumSalesQuantityBetween(Instant fromInclusive, Instant toExclusive) {
        return orderQueryApi.sumSalesQuantityBetween(fromInclusive, toExclusive);
    }

    @Override
    public long countCustomersCreatedBetween(Instant fromInclusive, Instant toExclusive) {
        return userQueryApi.countCustomersCreatedBetween(fromInclusive, toExclusive);
    }

    @Override
    public long countCustomersCreatedBefore(Instant beforeExclusive) {
        return userQueryApi.countCustomersCreatedBefore(beforeExclusive);
    }

    @Override
    public long countActiveSubscriptionsAt(Instant atInclusive) {
        return subscriptionQueryApi.countActiveSubscriptionsAt(atInclusive);
    }

    @Override
    public List<MonthlyRevenueAggregate> findMonthlyRevenueByYear(int year) {
        return orderQueryApi.findMonthlyRevenueByYear(year).stream()
                .map(p -> new MonthlyRevenueAggregate(p.month(), p.revenueAmount()))
                .toList();
    }

    @Override
    public List<TopProductAggregate> findTopProductsByYear(int year, String locale, int limit) {
        // locale is irrelevant now: the product name is the snapshot stored on the
        // order line at purchase time (no product_schema read).
        return orderQueryApi.findTopProductsByYear(year, limit).stream()
                .map(p -> new TopProductAggregate(p.productId(), p.name(), p.salesCount(), p.revenueAmount()))
                .toList();
    }

    @Override
    public Map<String, Long> countOrdersByStatusForYear(int year) {
        return orderQueryApi.countOrdersByStatusForYear(year);
    }

    @Override
    public List<DailyRevenueAggregate> findDailyRevenue(int days) {
        return orderQueryApi.findDailyRevenue(days).stream()
                .map(p -> new DailyRevenueAggregate(p.date(), p.revenueAmount(), p.salesCount()))
                .toList();
    }

    @Override
    public List<WeeklyRevenueAggregate> findWeeklyRevenue(int weeks) {
        return orderQueryApi.findWeeklyRevenue(weeks).stream()
                .map(p -> new WeeklyRevenueAggregate(p.weekStart(), p.revenueAmount(), p.salesCount()))
                .toList();
    }

    @Override
    public List<CategoryAvgCartAggregate> findCategoryAverageCartByYear(int year) {
        return orderQueryApi.findCategoryAverageCartByYear(year).stream()
                .map(p -> new CategoryAvgCartAggregate(p.category(), p.avgCartValue(), p.orderCount()))
                .toList();
    }

    @Override
    public List<CategorySalesAggregate> findCategorySalesByYear(int year) {
        return orderQueryApi.findCategorySalesByYear(year).stream()
                .map(p -> new CategorySalesAggregate(p.category(), p.revenueAmount(), p.quantity()))
                .toList();
    }

    @Override
    public List<Integer> findAvailableYears() {
        SortedSet<Integer> years = new TreeSet<>();
        years.addAll(orderQueryApi.findOrderYears());
        years.addAll(userQueryApi.findCustomerYears());
        years.addAll(subscriptionQueryApi.findSubscriptionYears());
        return List.copyOf(years);
    }
}
