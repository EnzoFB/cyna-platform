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
@Table(name = "payment_consent_log", schema = "payment_schema")
@Getter
@Setter
@NoArgsConstructor
public class PaymentConsentLogJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "label_version", nullable = false, length = 32)
    private String labelVersion;

    @Column(name = "stripe_payment_method_id", length = 255)
    private String stripePaymentMethodId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "given_at", nullable = false)
    private Instant givenAt;
}
