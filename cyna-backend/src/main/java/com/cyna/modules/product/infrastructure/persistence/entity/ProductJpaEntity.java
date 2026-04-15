package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "product_schema")
public class ProductJpaEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryJpaEntity category;

    @Column(name = "priority_level", nullable = false)
    private int priorityLevel;

    @Column(name = "service_description", nullable = false)
    private String serviceDescription;

    @Column(name = "technical_description", nullable = false)
    private String technicalDescription;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "highlight_points", nullable = false, columnDefinition = "jsonb")
    private List<String> highlightPoints;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProductJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public CategoryJpaEntity getCategory() { return category; }
    public void setCategory(CategoryJpaEntity category) { this.category = category; }

    public int getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(int priorityLevel) { this.priorityLevel = priorityLevel; }

    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }

    public String getTechnicalDescription() { return technicalDescription; }
    public void setTechnicalDescription(String technicalDescription) { this.technicalDescription = technicalDescription; }

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

    public List<String> getHighlightPoints() { return highlightPoints; }
    public void setHighlightPoints(List<String> highlightPoints) { this.highlightPoints = highlightPoints; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
