package com.cyna.modules.dashboard.application.query.getdashboard;

import com.cyna.modules.dashboard.domain.model.DashboardGoalSettings;
import com.cyna.modules.dashboard.domain.repository.DashboardGoalSettingsRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdminDashboardReadModelAssembler {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Paris");
    private static final Duration WEEK = Duration.ofDays(7);
    private static final Duration MONTH = Duration.ofDays(30);
    private static final Duration QUARTER = Duration.ofDays(90);
    private static final int DAILY_SALES_DAYS = 7;
    private static final int WEEKLY_SALES_WEEKS = 5;

    private final AdminDashboardQueryPort queryPort;
    private final DashboardGoalSettingsRepository goalSettingsRepository;

    public AdminDashboardReadModelAssembler(
            AdminDashboardQueryPort queryPort,
            DashboardGoalSettingsRepository goalSettingsRepository
    ) {
        this.queryPort = queryPort;
        this.goalSettingsRepository = goalSettingsRepository;
    }

    public AdminDashboardReadModel buildDashboard(Integer requestedYear) {
        int currentYear = Year.now(BUSINESS_ZONE).getValue();

        List<Integer> availableYears = new ArrayList<>(queryPort.findAvailableYears());
        if (!availableYears.contains(currentYear)) {
            availableYears.add(currentYear);
        }
        if (requestedYear != null && !availableYears.contains(requestedYear)) {
            availableYears.add(requestedYear);
        }
        availableYears = availableYears.stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        List<Integer> yearsToBuild = requestedYear == null
                ? availableYears
                : List.of(requestedYear);

        List<DashboardYearReadModel> years = yearsToBuild.stream()
                .map(this::buildYear)
                .toList();

        return new AdminDashboardReadModel(currentYear, availableYears, years);
    }

    public DashboardYearReadModel buildYear(int year) {
        int currentYear = Year.now(BUSINESS_ZONE).getValue();

        TimeWindow fiscalWindow = yearWindow(year);
        Instant anchor = year == currentYear
                ? Instant.now()
                : fiscalWindow.end().minusMillis(1);
        Instant progressionEnd = year == currentYear ? anchor : fiscalWindow.end();

        long revenueValue = queryPort.sumRevenueBetween(fiscalWindow.start(), progressionEnd);
        long salesValue = queryPort.sumSalesQuantityBetween(fiscalWindow.start(), progressionEnd);
        long clientsValue = queryPort.countCustomersCreatedBefore(progressionEnd);
        long newClientsProgressValue = queryPort.countCustomersCreatedBetween(fiscalWindow.start(), progressionEnd);
        long activeSubscriptionsValue = queryPort.countActiveSubscriptionsAt(anchor);

        List<Long> monthlyRevenueActual = toMonthlyArray(queryPort.findMonthlyRevenueByYear(year));
        DashboardGoalSettings goalSettings = resolveGoalSettings(
                year,
                revenueValue,
                newClientsProgressValue,
                monthlyRevenueActual
        );

        Map<String, DashboardMetricReadModel> metrics = new LinkedHashMap<>();
        metrics.put("revenue", new DashboardMetricReadModel(
                revenueValue,
                buildFlowComparisons(anchor, queryPort::sumRevenueBetween)
        ));
        metrics.put("clients", new DashboardMetricReadModel(
                clientsValue,
                buildFlowComparisons(anchor, queryPort::countCustomersCreatedBetween)
        ));
        metrics.put("sales", new DashboardMetricReadModel(
                salesValue,
                buildFlowComparisons(anchor, queryPort::sumSalesQuantityBetween)
        ));
        metrics.put("activeSubscriptions", new DashboardMetricReadModel(
                activeSubscriptionsValue,
                buildStockComparisons(anchor)
        ));

        List<DashboardTopProductReadModel> topProducts = queryPort.findTopProductsByYear(year, "fr", 8).stream()
                .map(item -> new DashboardTopProductReadModel(
                        item.productId() != null ? item.productId().toString() : "",
                        item.name() != null ? item.name() : "",
                        item.salesCount(),
                        item.revenueAmount()
                ))
                .toList();

        Map<String, Long> ordersByStatus = queryPort.countOrdersByStatusForYear(year);

        // Day/week histograms are anchored on "now", so they are only meaningful
        // for the current fiscal year; past years expose empty series.
        List<DashboardSalesPointReadModel> dailySales;
        List<DashboardSalesPointReadModel> weeklySales;
        List<DashboardCategoryAvgCartReadModel> categoryAvgCartDaily;
        List<DashboardCategoryAvgCartReadModel> categoryAvgCartWeekly;
        List<DashboardCategorySalesReadModel> categorySalesDaily;
        List<DashboardCategorySalesReadModel> categorySalesWeekly;

        if (year == currentYear) {
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            LocalDate firstDay = today.minusDays(DAILY_SALES_DAYS - 1L);
            Instant dailyFrom = firstDay.atStartOfDay(BUSINESS_ZONE).toInstant();
            Instant dailyTo = today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();

            LocalDate currentWeekStart = today.with(java.time.temporal.WeekFields.ISO.dayOfWeek(), 1L);
            LocalDate firstWeekStart = currentWeekStart.minusWeeks(WEEKLY_SALES_WEEKS - 1L);
            Instant weeklyFrom = firstWeekStart.atStartOfDay(BUSINESS_ZONE).toInstant();
            Instant weeklyTo = currentWeekStart.plusWeeks(1).atStartOfDay(BUSINESS_ZONE).toInstant();

            dailySales = queryPort.findDailyRevenue(DAILY_SALES_DAYS).stream()
                    .map(p -> new DashboardSalesPointReadModel(p.date().toString(), p.revenueAmount(), p.salesCount()))
                    .toList();
            weeklySales = queryPort.findWeeklyRevenue(WEEKLY_SALES_WEEKS).stream()
                    .map(p -> new DashboardSalesPointReadModel(p.weekStart().toString(), p.revenueAmount(), p.salesCount()))
                    .toList();
            categoryAvgCartDaily = queryPort.findCategoryAverageCartBetween(dailyFrom, dailyTo).stream()
                    .map(p -> new DashboardCategoryAvgCartReadModel(p.category(), p.avgCartValue(), p.orderCount()))
                    .toList();
            categoryAvgCartWeekly = queryPort.findCategoryAverageCartBetween(weeklyFrom, weeklyTo).stream()
                    .map(p -> new DashboardCategoryAvgCartReadModel(p.category(), p.avgCartValue(), p.orderCount()))
                    .toList();
            categorySalesDaily = queryPort.findCategorySalesBetween(dailyFrom, dailyTo).stream()
                    .map(p -> new DashboardCategorySalesReadModel(p.category(), p.revenueAmount(), p.quantity()))
                    .toList();
            categorySalesWeekly = queryPort.findCategorySalesBetween(weeklyFrom, weeklyTo).stream()
                    .map(p -> new DashboardCategorySalesReadModel(p.category(), p.revenueAmount(), p.quantity()))
                    .toList();
        } else {
            dailySales = List.of();
            weeklySales = List.of();
            categoryAvgCartDaily = List.of();
            categoryAvgCartWeekly = List.of();
            categorySalesDaily = List.of();
            categorySalesWeekly = List.of();
        }

        List<DashboardCategoryAvgCartReadModel> categoryAvgCart = queryPort.findCategoryAverageCartByYear(year).stream()
                .map(p -> new DashboardCategoryAvgCartReadModel(p.category(), p.avgCartValue(), p.orderCount()))
                .toList();
        List<DashboardCategorySalesReadModel> categorySales = queryPort.findCategorySalesByYear(year).stream()
                .map(p -> new DashboardCategorySalesReadModel(p.category(), p.revenueAmount(), p.quantity()))
                .toList();

        return new DashboardYearReadModel(
                year,
                metrics,
                new DashboardGoalReadModel(revenueValue, goalSettings.revenueTargetValue()),
                new DashboardGoalReadModel(newClientsProgressValue, goalSettings.clientsTargetValue()),
                goalSettings.monthlyRevenueGoal(),
                monthlyRevenueActual,
                topProducts,
                ordersByStatus != null ? ordersByStatus : Map.of(),
                dailySales,
                weeklySales,
                categoryAvgCart,
                categorySales,
                categoryAvgCartDaily,
                categoryAvgCartWeekly,
                categorySalesDaily,
                categorySalesWeekly
        );
    }

    private DashboardGoalSettings resolveGoalSettings(
            int year,
            long revenueProgressValue,
            long clientsProgressValue,
            List<Long> monthlyRevenueActual
    ) {
        return goalSettingsRepository.findByFiscalYear(year)
                .map(existing -> alignExistingSettings(existing, year))
                .orElseGet(() -> {
                    long defaultRevenueTarget = defaultRevenueTarget(revenueProgressValue, monthlyRevenueActual);
                    long defaultClientsTarget = defaultClientsTarget(clientsProgressValue);
                    return new DashboardGoalSettings(
                            year,
                            defaultRevenueTarget,
                            defaultClientsTarget,
                            DashboardGoalSettings.distributeEvenly(defaultRevenueTarget)
                    );
                });
    }

    private DashboardGoalSettings alignExistingSettings(DashboardGoalSettings existing, int year) {
        DashboardGoalSettings normalized = new DashboardGoalSettings(
                year,
                Math.max(0, existing.revenueTargetValue()),
                Math.max(0, existing.clientsTargetValue()),
                existing.monthlyRevenueGoal()
        );

        long monthlyTotal = normalized.monthlyRevenueGoal().stream().mapToLong(Long::longValue).sum();
        if (normalized.revenueTargetValue() <= 0) {
            return normalized.withMonthlyRevenueGoal(List.of());
        }
        if (monthlyTotal == normalized.revenueTargetValue()) {
            return normalized;
        }
        return normalized.withRevenueTargetValue(normalized.revenueTargetValue());
    }

    private long defaultRevenueTarget(long revenueProgressValue, List<Long> monthlyRevenueActual) {
        long actualTotal = monthlyRevenueActual.stream().mapToLong(Long::longValue).sum();
        long baseline = Math.max(revenueProgressValue, actualTotal);
        if (baseline <= 0) {
            return 0;
        }
        return Math.round(baseline * 1.2d);
    }

    private long defaultClientsTarget(long clientsProgressValue) {
        if (clientsProgressValue <= 0) {
            return 0;
        }
        return Math.round(clientsProgressValue * 1.2d);
    }

    private DashboardComparisonReadModel buildFlowComparisons(Instant anchor, WindowMetric metric) {
        DeltaAndValue week    = computeFlowDeltaAndValue(anchor, WEEK,    metric);
        DeltaAndValue month   = computeFlowDeltaAndValue(anchor, MONTH,   metric);
        DeltaAndValue quarter = computeFlowDeltaAndValue(anchor, QUARTER, metric);
        return new DashboardComparisonReadModel(
                week.delta(),    week.value(),
                month.delta(),   month.value(),
                quarter.delta(), quarter.value()
        );
    }

    private DashboardComparisonReadModel buildStockComparisons(Instant anchor) {
        long currentValue  = queryPort.countActiveSubscriptionsAt(anchor);
        long weekAgoValue  = queryPort.countActiveSubscriptionsAt(anchor.minus(WEEK));
        long monthAgoValue = queryPort.countActiveSubscriptionsAt(anchor.minus(MONTH));
        long quarterAgoValue = queryPort.countActiveSubscriptionsAt(anchor.minus(QUARTER));
        return new DashboardComparisonReadModel(
                percentageDelta(currentValue, weekAgoValue),     weekAgoValue,
                percentageDelta(currentValue, monthAgoValue),    monthAgoValue,
                percentageDelta(currentValue, quarterAgoValue),  quarterAgoValue
        );
    }

    private DeltaAndValue computeFlowDeltaAndValue(Instant anchor, Duration window, WindowMetric metric) {
        Instant currentStart  = anchor.minus(window);
        Instant previousStart = currentStart.minus(window);
        long current  = metric.value(currentStart, anchor);
        long previous = metric.value(previousStart, currentStart);
        return new DeltaAndValue(percentageDelta(current, previous), current);
    }

    private record DeltaAndValue(double delta, long value) {}

    private double percentageDelta(long current, long previous) {
        if (previous == 0L) {
            return current > 0L ? 100.0d : 0.0d;
        }
        double delta = ((double) (current - previous) / (double) previous) * 100.0d;
        return Math.round(delta * 10.0d) / 10.0d;
    }

    private List<Long> toMonthlyArray(List<MonthlyRevenueAggregate> aggregates) {
        List<Long> monthly = new ArrayList<>(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
        for (MonthlyRevenueAggregate aggregate : aggregates) {
            int monthIndex = aggregate.month() - 1;
            if (monthIndex >= 0 && monthIndex < 12) {
                monthly.set(monthIndex, Math.max(0L, aggregate.revenueAmount()));
            }
        }
        return List.copyOf(monthly);
    }

    private TimeWindow yearWindow(int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = startDate.plusYears(1);
        Instant start = startDate.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant end = endDate.atStartOfDay(BUSINESS_ZONE).toInstant();
        return new TimeWindow(start, end);
    }

    @FunctionalInterface
    private interface WindowMetric {
        long value(Instant fromInclusive, Instant toExclusive);
    }

    private record TimeWindow(Instant start, Instant end) {
    }
}
