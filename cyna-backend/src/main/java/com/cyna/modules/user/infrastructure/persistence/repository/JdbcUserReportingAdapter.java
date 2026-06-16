package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.application.api.UserReportingPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Customer analytics over {@code user_schema} only — exposed to other modules
 * through {@code UserQueryApi}.
 */
@Repository
public class JdbcUserReportingAdapter implements UserReportingPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcUserReportingAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
    public List<Integer> findCustomerYears() {
        String sql = """
                SELECT DISTINCT EXTRACT(YEAR FROM u.created_at)::int AS source_year
                FROM user_schema.users u
                WHERE u.role = 'CUSTOMER'
                ORDER BY source_year
                """;
        return jdbcTemplate.getJdbcTemplate().query(sql, (rs, rowNum) -> rs.getInt("source_year"));
    }

    private Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
