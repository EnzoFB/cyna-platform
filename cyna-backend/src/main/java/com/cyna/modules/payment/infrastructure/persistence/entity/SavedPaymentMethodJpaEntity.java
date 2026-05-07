package com.cyna.modules.payment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_payment_methods", schema = "payment_schema")
@Getter
@Setter
@NoArgsConstructor
public class SavedPaymentMethodJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "stripe_payment_method_id", nullable = false, length = 255)
    private String stripePaymentMethodId;

    @Column(name = "brand", nullable = false, length = 50)
    private String brand;

    @Column(name = "last4", nullable = false, length = 4)
    private String last4;

    @Column(name = "exp_month", nullable = false, length = 2)
    private String expMonth;

    @Column(name = "exp_year", nullable = false, length = 4)
    private String expYear;

    @Column(name = "holder_name", length = 255)
    private String holderName;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
