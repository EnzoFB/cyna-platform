package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "offer_carousel_settings", schema = "product_schema")
public class OfferCarouselSettingsJpaEntity {

    @Id
    private Short id;

    @Column(name = "fixed_text_fr", nullable = false)
    private String fixedTextFr;

    @Column(name = "fixed_text_en", nullable = false)
    private String fixedTextEn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public OfferCarouselSettingsJpaEntity() {
    }

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public String getFixedTextFr() {
        return fixedTextFr;
    }

    public void setFixedTextFr(String fixedTextFr) {
        this.fixedTextFr = fixedTextFr;
    }

    public String getFixedTextEn() {
        return fixedTextEn;
    }

    public void setFixedTextEn(String fixedTextEn) {
        this.fixedTextEn = fixedTextEn;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
