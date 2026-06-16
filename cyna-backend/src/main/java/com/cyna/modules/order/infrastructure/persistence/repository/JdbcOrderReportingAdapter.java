package com.cyna.modules.order.infrastructure.persistence.repository;

import com.cyna.modules.order.application.api.OrderQueryApi.MonthlyRevenuePoint;
import com.cyna.modules.order.application.api.OrderQueryApi.TopProductPoint;
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
}
