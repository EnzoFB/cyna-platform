package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "offer_carousel_settings_translations", schema = "product_schema")
public class OfferCarouselSettingsTranslationJpaEntity {

    @EmbeddedId
    private OfferCarouselSettingsTranslationId id = new OfferCarouselSettingsTranslationId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("settingsId")
    @JoinColumn(name = "settings_id")
    private OfferCarouselSettingsJpaEntity settings;

    @Column(name = "fixed_text", nullable = false, columnDefinition = "text")
    private String fixedText;

    public OfferCarouselSettingsTranslationJpaEntity() {}

    // ── Convenience factory ───────────────────────────────────────────────────

    public static OfferCarouselSettingsTranslationJpaEntity of(OfferCarouselSettingsJpaEntity settings,
                                                                String locale,
                                                                String fixedText) {
        var t = new OfferCarouselSettingsTranslationJpaEntity();
        t.id        = new OfferCarouselSettingsTranslationId(settings.getId(), locale);
        t.settings  = settings;
        t.fixedText = fixedText;
        return t;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public OfferCarouselSettingsTranslationId getId()                       { return id; }
    public void setId(OfferCarouselSettingsTranslationId id)                { this.id = id; }

    public String getLocale()                                               { return id.getLocale(); }

    public OfferCarouselSettingsJpaEntity getSettings()                     { return settings; }
    public void setSettings(OfferCarouselSettingsJpaEntity settings)        { this.settings = settings; }

    public String getFixedText()                                            { return fixedText; }
    public void setFixedText(String fixedText)                              { this.fixedText = fixedText; }
}
