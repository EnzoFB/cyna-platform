package com.cyna.modules.subscription.infrastructure.persistence.repository;

import com.cyna.modules.subscription.application.api.SubscriptionReportingPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Subscription analytics over {@code subscription_schema} only — exposed to
 * other modules through {@code SubscriptionQueryApi}.
 */
@Repository
public class JdbcSubscriptionReportingAdapter implements SubscriptionReportingPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcSubscriptionReportingAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
    public List<Integer> findSubscriptionYears() {
        String sql = """
                SELECT DISTINCT EXTRACT(YEAR FROM s.created_at)::int AS source_year
                FROM subscription_schema.subscriptions s
                ORDER BY source_year
                """;
        return jdbcTemplate.getJdbcTemplate().query(sql, (rs, rowNum) -> rs.getInt("source_year"));
    }

    private Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
