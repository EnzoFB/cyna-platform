package com.cyna.modules.dashboard.infrastructure.persistence.repository;

import com.cyna.modules.dashboard.application.query.getdashboard.AdminDashboardQueryPort;
import com.cyna.modules.dashboard.application.query.getdashboard.MonthlyRevenueAggregate;
import com.cyna.modules.dashboard.application.query.getdashboard.TopProductAggregate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcAdminDashboardQueryAdapter implements AdminDashboardQueryPort {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Paris");

    private static final String REVENUE_STATUSES_SQL = "'PAID','FULFILLED'";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAdminDashboardQueryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long sumRevenueBetween(Instant fromInclusive, Instant toExclusive) {
        String sql = """
                SELECT COALESCE(SUM(o.total_amount), 0)
                FROM order_schema.orders o
                WHERE o.status IN (""" + REVENUE_STATUSES_SQL + """
                )
                  AND o.created_at >= :fromInclusive
                  AND o.created_at < :toExclusive
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromInclusive", toTimestamp(fromInclusive))
                .addValue("toExclusive", toTimestamp(toExclusive));

        BigDecimal value = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
        if (value == null) {
            return 0L;
        }
        return value.setScale(0, RoundingMode.HALF_UP).longValue();
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
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromInclusive", toTimestamp(fromInclusive))
                .addValue("toExclusive", toTimestamp(toExclusive));

        Long value = jdbcTemplate.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }

    @Override
    public long countCustomersCreatedBetween(Instant fromInclusive, Instant toExclusive) {
        String sql = """
                SELECT COUNT(*)
                FROM user_schema.users u
                WHERE u.role = 'CUSTOMER'
                  AND u.created_at >= :fromInclusive
                  AND u.created_at < :toExclusive
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromInclusive", toTimestamp(fromInclusive))
                .addValue("toExclusive", toTimestamp(toExclusive));
        Long value = jdbcTemplate.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }

    @Override
    public long countCustomersCreatedBefore(Instant beforeExclusive) {
        String sql = """
                SELECT COUNT(*)
                FROM user_schema.users u
                WHERE u.role = 'CUSTOMER'
                  AND u.created_at < :beforeExclusive
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("beforeExclusive", toTimestamp(beforeExclusive));
        Long value = jdbcTemplate.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }

    @Override
    public long countActiveSubscriptionsAt(Instant atInclusive) {
        String sql = """
                SELECT COUNT(*)
                FROM subscription_schema.subscriptions s
                WHERE s.status = 'ACTIVE'
                  AND s.start_at <= :atInclusive
                  AND s.end_at > :atInclusive
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("atInclusive", toTimestamp(atInclusive));
        Long value = jdbcTemplate.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }

    @Override
    public List<MonthlyRevenueAggregate> findMonthlyRevenueByYear(int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        Instant fromInclusive = yearStart.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant toExclusive = yearStart.plusYears(1).atStartOfDay(BUSINESS_ZONE).toInstant();

        String sql = """
                SELECT EXTRACT(MONTH FROM o.created_at)::int AS month,
                       COALESCE(SUM(o.total_amount), 0) AS revenue_amount
                FROM order_schema.orders o
                WHERE o.status IN (""" + REVENUE_STATUSES_SQL + """
                )
                  AND o.created_at >= :fromInclusive
                  AND o.created_at < :toExclusive
                GROUP BY EXTRACT(MONTH FROM o.created_at)::int
                ORDER BY month
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromInclusive", toTimestamp(fromInclusive))
                .addValue("toExclusive", toTimestamp(toExclusive));

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new MonthlyRevenueAggregate(
                rs.getInt("month"),
                rs.getBigDecimal("revenue_amount").setScale(0, RoundingMode.HALF_UP).longValue()
        ));
    }

    @Override
    public List<TopProductAggregate> findTopProductsByYear(int year, String locale, int limit) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        Instant fromInclusive = yearStart.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant toExclusive = yearStart.plusYears(1).atStartOfDay(BUSINESS_ZONE).toInstant();

        String sql = """
                SELECT p.id AS product_id,
                       COALESCE(NULLIF(pt.name, ''), p.id::text) AS product_name,
                       COALESCE(SUM(CASE
                           WHEN o.status IN (""" + REVENUE_STATUSES_SQL + """
                           )
                                AND o.created_at >= :fromInclusive
                                AND o.created_at < :toExclusive
                           THEN ol.quantity
                           ELSE 0
                       END), 0) AS sales_count,
                       COALESCE(SUM(CASE
                           WHEN o.status IN (""" + REVENUE_STATUSES_SQL + """
                           )
                                AND o.created_at >= :fromInclusive
                                AND o.created_at < :toExclusive
                           THEN (ol.quantity * ol.unit_price)
                           ELSE 0
                       END), 0) AS revenue_amount,
                       p.priority_level
                FROM product_schema.products p
                LEFT JOIN product_schema.product_translations pt
                       ON pt.product_id = p.id
                      AND pt.locale = :locale
                LEFT JOIN order_schema.order_lines ol
                       ON ol.product_id = p.id
                LEFT JOIN order_schema.orders o
                       ON o.id = ol.order_id
                WHERE p.is_available = TRUE OR p.is_published = TRUE
                GROUP BY p.id, pt.name, p.priority_level
                HAVING COALESCE(SUM(CASE
                           WHEN o.status IN (""" + REVENUE_STATUSES_SQL + """
                           )
                                AND o.created_at >= :fromInclusive
                                AND o.created_at < :toExclusive
                           THEN ol.quantity
                           ELSE 0
                       END), 0) > 0
                    OR COALESCE(SUM(CASE
                           WHEN o.status IN (""" + REVENUE_STATUSES_SQL + """
                           )
                                AND o.created_at >= :fromInclusive
                                AND o.created_at < :toExclusive
                           THEN (ol.quantity * ol.unit_price)
                           ELSE 0
                       END), 0) > 0
                ORDER BY sales_count DESC, revenue_amount DESC, p.priority_level ASC
                LIMIT :limit
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromInclusive", toTimestamp(fromInclusive))
                .addValue("toExclusive", toTimestamp(toExclusive))
                .addValue("locale", locale == null || locale.isBlank() ? "fr" : locale)
                .addValue("limit", Math.max(1, limit));

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            UUID productId = rs.getObject("product_id", UUID.class);
            String name = rs.getString("product_name");
            long salesCount = rs.getLong("sales_count");
            BigDecimal revenueRaw = rs.getBigDecimal("revenue_amount");
            long revenueAmount = revenueRaw == null ? 0L : revenueRaw.setScale(0, RoundingMode.HALF_UP).longValue();
            return new TopProductAggregate(productId, name, salesCount, revenueAmount);
        });
    }

    @Override
    public List<Integer> findAvailableYears() {
        String sql = """
                SELECT DISTINCT source_year
                FROM (
                    SELECT EXTRACT(YEAR FROM o.created_at)::int AS source_year
                    FROM order_schema.orders o
                    UNION ALL
                    SELECT EXTRACT(YEAR FROM u.created_at)::int AS source_year
                    FROM user_schema.users u
                    WHERE u.role = 'CUSTOMER'
                    UNION ALL
                    SELECT EXTRACT(YEAR FROM s.created_at)::int AS source_year
                    FROM subscription_schema.subscriptions s
                ) years
                ORDER BY source_year
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("source_year"));
    }

    private Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
