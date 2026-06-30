package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "product_schema")
public class ProductJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryJpaEntity category;

    @Column(name = "priority_level", nullable = false)
    private int priorityLevel;

    @Column(name = "monthly_price", nullable = false)
    private BigDecimal monthlyPrice;

    @Column(name = "annual_price", nullable = false)
    private BigDecimal annualPrice;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

    @Column(name = "free_trial_days", nullable = false)
    private int freeTrialDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** All locale translations (fr, en, …). Loaded eagerly: products are always displayed with their names. */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<ProductTranslationJpaEntity> translations = new HashSet<>();

    public ProductJpaEntity() {}

    // ── Non-translatable fields ───────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CategoryJpaEntity getCategory() { return category; }
    public void setCategory(CategoryJpaEntity category) { this.category = category; }

    public int getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(int priorityLevel) { this.priorityLevel = priorityLevel; }

    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }

    public BigDecimal getAnnualPrice() { return annualPrice; }
    public void setAnnualPrice(BigDecimal annualPrice) { this.annualPrice = annualPrice; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean published) { isPublished = published; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public int getFreeTrialDays() { return freeTrialDays; }
    public void setFreeTrialDays(int freeTrialDays) { this.freeTrialDays = freeTrialDays; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // ── Translations ──────────────────────────────────────────────────────────

    public Set<ProductTranslationJpaEntity> getTranslations() { return translations; }
    public void setTranslations(Set<ProductTranslationJpaEntity> translations) {
        this.translations.clear();
        if (translations != null) {
            this.translations.addAll(translations);
        }
    }
}
