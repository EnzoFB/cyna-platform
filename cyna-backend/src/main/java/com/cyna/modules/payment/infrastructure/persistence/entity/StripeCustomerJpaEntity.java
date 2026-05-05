package com.cyna.modules.payment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stripe_customers", schema = "payment_schema")
@Getter
@Setter
@NoArgsConstructor
public class StripeCustomerJpaEntity {

    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "stripe_customer_id", nullable = false, length = 255)
    private String stripeCustomerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
