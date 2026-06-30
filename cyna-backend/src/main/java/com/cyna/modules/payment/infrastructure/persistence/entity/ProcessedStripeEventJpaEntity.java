package com.cyna.modules.payment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "processed_stripe_events", schema = "payment_schema")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedStripeEventJpaEntity {

    @Id
    @Column(name = "event_id", length = 255)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
