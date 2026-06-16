package com.cyna.modules.order.application.query.reporting;

import com.cyna.modules.order.application.api.OrderQueryApi.MonthlyRevenuePoint;
import com.cyna.modules.order.application.api.OrderQueryApi.TopProductPoint;

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

    List<Integer> findOrderYears();
}
