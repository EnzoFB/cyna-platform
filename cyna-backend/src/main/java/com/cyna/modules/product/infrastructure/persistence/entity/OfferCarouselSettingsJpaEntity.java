package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "offer_carousel_settings", schema = "product_schema")
public class OfferCarouselSettingsJpaEntity {

    @Id
    private Short id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "settings", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<OfferCarouselSettingsTranslationJpaEntity> translations = new HashSet<>();

    public OfferCarouselSettingsJpaEntity() {}

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Short getId()                                                    { return id; }
    public void setId(Short id)                                             { this.id = id; }

    public Instant getCreatedAt()                                           { return createdAt; }
    public void setCreatedAt(Instant createdAt)                             { this.createdAt = createdAt; }

    public Instant getUpdatedAt()                                           { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt)                             { this.updatedAt = updatedAt; }

    public Set<OfferCarouselSettingsTranslationJpaEntity> getTranslations() { return translations; }

    /** Clear-then-addAll so Hibernate orphan removal deletes stale rows. */
    public void setTranslations(Set<OfferCarouselSettingsTranslationJpaEntity> translations) {
        this.translations.clear();
        if (translations != null) this.translations.addAll(translations);
    }
}
