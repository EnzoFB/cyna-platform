package com.cyna.modules.order.application.query.reporting;

import com.cyna.modules.order.application.api.OrderQueryApi.CategoryAvgCartPoint;
import com.cyna.modules.order.application.api.OrderQueryApi.CategorySalesPoint;
import com.cyna.modules.order.application.api.OrderQueryApi.DailyRevenuePoint;
import com.cyna.modules.order.application.api.OrderQueryApi.MonthlyRevenuePoint;
import com.cyna.modules.order.application.api.OrderQueryApi.TopProductPoint;
import com.cyna.modules.order.application.api.OrderQueryApi.WeeklyRevenuePoint;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Internal reporting port for order analytics. Implemented by an infrastructure
 * adapter that reads {@code order_schema} only. Exposed to other modules through
 * {@code OrderQueryApi}, so reporting never reaches across schemas.
 */
public interface OrderReportingPort {

    long sumRevenueBetween(Instant fromInclusive, Instant toExclusive);

    long sumSalesQuantityBetween(Instant fromInclusive, Instant toExclusive);

    List<MonthlyRevenuePoint> findMonthlyRevenueByYear(int year);

    List<TopProductPoint> findTopProductsByYear(int year, int limit);

    Map<String, Long> countOrdersByStatusForYear(int year);

    List<DailyRevenuePoint> findDailyRevenue(int days);

    List<WeeklyRevenuePoint> findWeeklyRevenue(int weeks);

    List<CategoryAvgCartPoint> findCategoryAverageCartByYear(int year);

    List<CategorySalesPoint> findCategorySalesByYear(int year);

    List<Integer> findOrderYears();
}
