package com.cyna.modules.payment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_tax_snapshots", schema = "payment_schema")
@Getter
@Setter
@NoArgsConstructor
public class OrderTaxSnapshotJpaEntity {

    @Id
    @Column(name = "order_id", columnDefinition = "uuid")
    private UUID orderId;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "subtotal_ht", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotalHt;

    @Column(name = "vat_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal vatAmount;

    @Column(name = "total_ttc", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalTtc;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "reverse_charge", nullable = false)
    private boolean reverseCharge;

    @Column(name = "captured_at", nullable = false, updatable = false)
    private Instant capturedAt;
}
