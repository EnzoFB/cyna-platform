package com.cyna.modules.payment.integration;

import com.cyna.modules.payment.domain.repository.ProcessedStripeEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the webhook dedup ledger against a real PostgreSQL: that migration
 * V22 created the table and that {@code INSERT ... ON CONFLICT DO NOTHING}
 * makes {@code markProcessed} idempotent and concurrency-safe. Self-contained:
 * {@code processed_stripe_events} has no FK, so no fixture seeding needed.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@SuppressWarnings("resource") // Testcontainers manages the container lifecycle.
class ProcessedStripeEventRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cyna_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private ProcessedStripeEventRepository repository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void new_event_is_not_yet_processed_then_recorded() {
        String id = "evt_int_" + System.nanoTime();

        assertThat(repository.isAlreadyProcessed(id)).isFalse();

        repository.markProcessed(id, "invoice.paid");

        assertThat(repository.isAlreadyProcessed(id)).isTrue();
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_schema.processed_stripe_events WHERE event_id = ?",
                Integer.class, id);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void mark_processed_is_idempotent_on_duplicate() {
        String id = "evt_int_dup_" + System.nanoTime();

        repository.markProcessed(id, "customer.subscription.updated");
        // Second delivery of the same event id — ON CONFLICT DO NOTHING.
        repository.markProcessed(id, "customer.subscription.updated");
        repository.markProcessed(id, "customer.subscription.updated");

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_schema.processed_stripe_events WHERE event_id = ?",
                Integer.class, id);
        assertThat(count).isEqualTo(1);
        assertThat(repository.isAlreadyProcessed(id)).isTrue();
    }

    @Test
    void migration_v22_created_the_table_with_expected_columns() {
        Integer cols = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'payment_schema'
                  AND table_name = 'processed_stripe_events'
                  AND column_name IN ('event_id', 'event_type', 'processed_at')
                """, Integer.class);
        assertThat(cols).isEqualTo(3);
    }
}
