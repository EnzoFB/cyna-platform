package com.cyna.modules.order.infrastructure.persistence.repository;

import com.cyna.modules.order.application.api.OrderQueryApi.CategoryAvgCartPoint;
import com.cyna.modules.order.application.api.OrderQueryApi.CategorySalesPoint;
import com.cyna.modules.order.application.api.OrderQueryApi.DailyRevenuePoint;
import com.cyna.modules.order.application.api.OrderQueryApi.MonthlyRevenuePoint;
import com.cyna.modules.order.application.api.OrderQueryApi.TopProductPoint;
import com.cyna.modules.order.application.api.OrderQueryApi.WeeklyRevenuePoint;
import com.cyna.modules.order.application.query.reporting.OrderReportingPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Order analytics over {@code order_schema} only — the order module owns these
 * aggregates and exposes them through {@code OrderQueryApi}, so the dashboard
 * never reads order tables across a schema boundary.
 */
@Repository
public class JdbcOrderReportingAdapter implements OrderReportingPort {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Paris");
    private static final String REVENUE_STATUSES_SQL = "'PAID','FULFILLED'";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcOrderReportingAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long sumRevenueBetween(Instant fromInclusive, Instant toExclusive) {
        String sql = """
                SELECT COALESCE(SUM(o.subtotal_amount), 0)
                FROM order_schema.orders o
                WHERE o.status IN (""" + REVENUE_STATUSES_SQL + """
                )
                  AND o.created_at >= :fromInclusive
                  AND o.created_at < :toExclusive
                """;
        BigDecimal value = jdbcTemplate.queryForObject(sql, window(fromInclusive, toExclusive), BigDecimal.class);
        return value == null ? 0L : value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    @Override
    public long sumSalesQuantityBetween(Instant fromInclusive, Instant toExclusive) {
        String sql = """
                SELECT COALESCE(SUM(ol.quantity), 0)
                FROM order_schema.order_lines ol
                JOIN order_schema.orders o ON o.id = ol.order_id
                WHERE o.status IN (""" + REVENUE_STATUSES_SQL + """
                )
                  AND o.created_at >= :fromInclusive
                  AND o.created_at < :toExclusive
                """;
        Long value = jdbcTemplate.queryForObject(sql, window(fromInclusive, toExclusive), Long.class);
        return value == null ? 0L : value;
    }

    @Override
    public List<MonthlyRevenuePoint> findMonthlyRevenueByYear(int year) {
        String sql = """
                SELECT EXTRACT(MONTH FROM o.created_at)::int AS month,
                       COALESCE(SUM(o.subtotal_amount), 0) AS revenue_amount
                FROM order_schema.orders o
                WHERE o.status IN (""" + REVENUE_STATUSES_SQL + """
                )
                  AND o.created_at >= :fromInclusive
                  AND o.created_at < :toExclusive
                GROUP BY EXTRACT(MONTH FROM o.created_at)::int
                ORDER BY month
                """;
        return jdbcTemplate.query(sql, yearWindow(year), (rs, rowNum) -> new MonthlyRevenuePoint(
                rs.getInt("month"),
                rs.getBigDecimal("revenue_amount").setScale(0, RoundingMode.HALF_UP).longValue()
        ));
    }

    @Override
    public List<TopProductPoint> findTopProductsByYear(int year, int limit) {
        String sql = """
                SELECT ol.product_id AS product_id,
                       MAX(ol.product_name) AS product_name,
                       SUM(ol.quantity) AS sales_count,
                       SUM(ol.quantity * ol.unit_price) AS revenue_amount
                FROM order_schema.order_lines ol
                JOIN order_schema.orders o ON o.id = ol.order_id
                WHERE o.status IN (""" + REVENUE_STATUSES_SQL + """
                )
                  AND o.created_at >= :fromInclusive
                  AND o.created_at < :toExclusive
                GROUP BY ol.product_id
                HAVING SUM(ol.quantity) > 0
                ORDER BY sales_count DESC, revenue_amount DESC
                LIMIT :limit
                """;
        MapSqlParameterSource params = yearWindow(year).addValue("limit", Math.max(1, limit));
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            UUID productId = rs.getObject("product_id", UUID.class);
            String name = rs.getString("product_name");
            long salesCount = rs.getLong("sales_count");
            BigDecimal revenueRaw = rs.getBigDecimal("revenue_amount");
            long revenueAmount = revenueRaw == null ? 0L : revenueRaw.setScale(0, RoundingMode.HALF_UP).longValue();
            return new TopProductPoint(productId, name, salesCount, revenueAmount);
        });
    }

    @Override
    public Map<String, Long> countOrdersByStatusForYear(int year) {
        String sql = """
                SELECT o.status, COUNT(*) AS order_count
                FROM order_schema.orders o
                WHERE o.created_at >= :fromInclusive
                  AND o.created_at < :toExclusive
                GROUP BY o.status
                """;
        Map<String, Long> result = new LinkedHashMap<>();
        for (String status : List.of("PENDING", "CONFIRMED", "PAID", "FULFILLED", "CANCELLED")) {
            result.put(status, 0L);
        }
        jdbcTemplate.query(sql, yearWindow(year), (rs, rowNum) -> {
            result.put(rs.getString("status"), rs.getLong("order_count"));
            return null;
        });
        return Collections.unmodifiableMap(result);
    }

    @Override
    public List<DailyRevenuePoint> findDailyRevenue(int days) {
        int span = Math.max(1, days);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate firstDay = today.minusDays(span - 1L);
        Instant fromInclusive = firstDay.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant toExclusive = today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();

        String sql = """
                SELECT (date_trunc('day', o.created_at AT TIME ZONE :zone))::date AS bucket,
                       COALESCE(SUM(o.subtotal_amount), 0) AS revenue_amount,
                       COALESCE(SUM(ol.line_quantity), 0) AS sales_count
                FROM order_schema.orders o
                LEFT JOIN (
                    SELECT order_id, SUM(quantity) AS line_quantity
                    FROM order_schema.order_lines
                    GROUP BY order_id
                ) ol ON ol.order_id = o.id
                WHERE o.status IN (""" + REVENUE_STATUSES_SQL + """
                )
                  AND o.created_at >= :fromInclusive
                  AND o.created_at < :toExclusive
                GROUP BY bucket
                """;
        MapSqlParameterSource params = window(fromInclusive, toExclusive).addValue("zone", BUSINESS_ZONE.getId());
        Map<LocalDate, long[]> byDay = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            byDay.put(rs.getObject("bucket", LocalDate.class), new long[]{
                    roundToLong(rs.getBigDecimal("revenue_amount")),
                    rs.getLong("sales_count")
            });
            return null;
        });

        List<DailyRevenuePoint> result = new java.util.ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            LocalDate date = firstDay.plusDays(i);
            long[] values = byDay.getOrDefault(date, new long[]{0L, 0L});
            result.add(new DailyRevenuePoint(date, values[0], values[1]));
        }
        return result;
    }

    @Override
    public List<WeeklyRevenuePoint> findWeeklyRevenue(int weeks) {
        int span = Math.max(1, weeks);
        LocalDate currentWeekStart = LocalDate.now(BUSINESS_ZONE)
                .with(java.time.temporal.WeekFields.ISO.dayOfWeek(), 1L);
        LocalDate firstWeekStart = currentWeekStart.minusWeeks(span - 1L);
        Instant fromInclusive = firstWeekStart.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant toExclusive = currentWeekStart.plusWeeks(1).atStartOfDay(BUSINESS_ZONE).toInstant();

        String sql = """
                SELECT (date_trunc('week', o.created_at AT TIME ZONE :zone))::date AS bucket,
                       COALESCE(SUM(o.subtotal_amount), 0) AS revenue_amount,
                       COALESCE(SUM(ol.line_quantity), 0) AS sales_count
                FROM order_schema.orders o
                LEFT JOIN (
                    SELECT order_id, SUM(quantity) AS line_quantity
                    FROM order_schema.order_lines
                    GROUP BY order_id
                ) ol ON ol.order_id = o.id
                WHERE o.status IN (""" + REVENUE_STATUSES_SQL + """
                )
                  AND o.created_at >= :fromInclusive
                  AND o.created_at < :toExclusive
                GROUP BY bucket
                """;
        MapSqlParameterSource params = window(fromInclusive, toExclusive).addValue("zone", BUSINESS_ZONE.getId());
        Map<LocalDate, long[]> byWeek = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            byWeek.put(rs.getObject("bucket", LocalDate.class), new long[]{
                    roundToLong(rs.getBigDecimal("revenue_amount")),
                    rs.getLong("sales_count")
            });
            return null;
        });

        List<WeeklyRevenuePoint> result = new java.util.ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            LocalDate weekStart = firstWeekStart.plusWeeks(i);
            long[] values = byWeek.getOrDefault(weekStart, new long[]{0L, 0L});
            result.add(new WeeklyRevenuePoint(weekStart, values[0], values[1]));
        }
        return result;
    }

    @Override
    public List<CategoryAvgCartPoint> findCategoryAverageCartByYear(int year) {
        String sql = """
                SELECT ol.product_category AS category,
                       SUM(ol.quantity * ol.unit_price) AS category_revenue,
                       COUNT(DISTINCT ol.order_id) AS order_count
                FROM order_schema.order_lines ol
                JOIN order_schema.orders o ON o.id = ol.order_id
                WHERE o.status IN (""" + REVENUE_STATUSES_SQL + """
                )
                  AND o.created_at >= :fromInclusive
                  AND o.created_at < :toExclusive
                GROUP BY ol.product_category
                HAVING COUNT(DISTINCT ol.order_id) > 0
                ORDER BY category_revenue DESC
                """;
        return jdbcTemplate.query(sql, yearWindow(year), (rs, rowNum) -> {
            long revenue = roundToLong(rs.getBigDecimal("category_revenue"));
            long orderCount = rs.getLong("order_count");
            long avgCart = orderCount > 0 ? Math.round((double) revenue / (double) orderCount) : 0L;
            return new CategoryAvgCartPoint(categoryLabel(rs.getString("category")), avgCart, orderCount);
        });
    }

    @Override
    public List<CategorySalesPoint> findCategorySalesByYear(int year) {
        String sql = """
                SELECT ol.product_category AS category,
                       SUM(ol.quantity * ol.unit_price) AS category_revenue,
                       SUM(ol.quantity) AS quantity
                FROM order_schema.order_lines ol
                JOIN order_schema.orders o ON o.id = ol.order_id
                WHERE o.status IN (""" + REVENUE_STATUSES_SQL + """
                )
                  AND o.created_at >= :fromInclusive
                  AND o.created_at < :toExclusive
                GROUP BY ol.product_category
                HAVING SUM(ol.quantity) > 0
                ORDER BY category_revenue DESC
                """;
        return jdbcTemplate.query(sql, yearWindow(year), (rs, rowNum) -> new CategorySalesPoint(
                categoryLabel(rs.getString("category")),
                roundToLong(rs.getBigDecimal("category_revenue")),
                rs.getLong("quantity")
        ));
    }

    @Override
    public List<Integer> findOrderYears() {
        String sql = """
                SELECT DISTINCT EXTRACT(YEAR FROM o.created_at)::int AS source_year
                FROM order_schema.orders o
                ORDER BY source_year
                """;
        return jdbcTemplate.getJdbcTemplate().query(sql, (rs, rowNum) -> rs.getInt("source_year"));
    }

    private MapSqlParameterSource window(Instant fromInclusive, Instant toExclusive) {
        return new MapSqlParameterSource()
                .addValue("fromInclusive", toTimestamp(fromInclusive))
                .addValue("toExclusive", toTimestamp(toExclusive));
    }

    private MapSqlParameterSource yearWindow(int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        Instant fromInclusive = yearStart.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant toExclusive = yearStart.plusYears(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        return window(fromInclusive, toExclusive);
    }

    private Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private long roundToLong(BigDecimal value) {
        return value == null ? 0L : value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private String categoryLabel(String rawCategory) {
        return rawCategory == null || rawCategory.isBlank() ? "UNKNOWN" : rawCategory;
    }
}
