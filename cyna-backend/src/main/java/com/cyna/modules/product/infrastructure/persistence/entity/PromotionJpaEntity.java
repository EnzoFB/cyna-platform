package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "promotions", schema = "product_schema")
public class PromotionJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;

    @Column(name = "discount_percent", nullable = false)
    private int discountPercent;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<PromotionTranslationJpaEntity> translations = new HashSet<>();

    public PromotionJpaEntity() {}

    // ── Accessors ─────────────────────────────────────────────────────────────

    public UUID getId()                           { return id; }
    public void setId(UUID id)                    { this.id = id; }

    public ProductJpaEntity getProduct()          { return product; }
    public void setProduct(ProductJpaEntity p)    { this.product = p; }

    public int getDiscountPercent()               { return discountPercent; }
    public void setDiscountPercent(int v)         { this.discountPercent = v; }

    public Instant getStartAt()                   { return startAt; }
    public void setStartAt(Instant v)             { this.startAt = v; }

    public Instant getEndAt()                     { return endAt; }
    public void setEndAt(Instant v)               { this.endAt = v; }

    public boolean isEnabled()                    { return enabled; }
    public void setEnabled(boolean v)             { this.enabled = v; }

    public Instant getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(Instant v)           { this.createdAt = v; }

    public Instant getUpdatedAt()                 { return updatedAt; }
    public void setUpdatedAt(Instant v)           { this.updatedAt = v; }

    public Set<PromotionTranslationJpaEntity> getTranslations() { return translations; }

    /** Clear-then-addAll so Hibernate orphan removal deletes stale rows. */
    public void setTranslations(Set<PromotionTranslationJpaEntity> translations) {
        this.translations.clear();
        if (translations != null) this.translations.addAll(translations);
    }
}
