package com.cyna.modules.payment.infrastructure.persistence.entity;

import com.cyna.modules.payment.domain.model.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payment_schema")
@Getter
@Setter
@NoArgsConstructor
public class PaymentJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "order_id", nullable = false, columnDefinition = "uuid")
    private UUID orderId;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "stripe_payment_intent_id", unique = true, length = 255)
    private String stripePaymentIntentId;

    @Column(name = "stripe_client_secret", length = 500)
    private String stripeClientSecret;

    @Column(name = "stripe_subscription_id", length = 255)
    private String stripeSubscriptionId;

    @Column(name = "stripe_schedule_id", length = 255)
    private String stripeScheduleId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
