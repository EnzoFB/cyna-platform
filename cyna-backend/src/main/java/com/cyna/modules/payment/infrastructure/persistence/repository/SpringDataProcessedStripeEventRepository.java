package com.cyna.modules.payment.infrastructure.persistence.repository;

import com.cyna.modules.payment.infrastructure.persistence.entity.ProcessedStripeEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataProcessedStripeEventRepository
        extends JpaRepository<ProcessedStripeEventJpaEntity, String> {

    /**
     * Atomic claim. {@code ON CONFLICT DO NOTHING} makes the database arbitrate
     * concurrent deliveries of the same event: exactly one INSERT wins (returns
     * 1), every duplicate returns 0. No SELECT-then-INSERT race window.
     */
    @Modifying
    @Query(value = """
            INSERT INTO payment_schema.processed_stripe_events (event_id, event_type, processed_at)
            VALUES (:eventId, :eventType, NOW())
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") String eventId, @Param("eventType") String eventType);
}
