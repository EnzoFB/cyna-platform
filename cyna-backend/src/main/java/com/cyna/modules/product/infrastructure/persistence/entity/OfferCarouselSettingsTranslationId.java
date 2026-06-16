package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class OfferCarouselSettingsTranslationId implements Serializable {

    @Column(name = "settings_id", nullable = false)
    private Short settingsId;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    public OfferCarouselSettingsTranslationId() {}

    public OfferCarouselSettingsTranslationId(Short settingsId, String locale) {
        this.settingsId = settingsId;
        this.locale     = locale;
    }

    public Short  getSettingsId()               { return settingsId; }
    public void   setSettingsId(Short settingsId) { this.settingsId = settingsId; }
    public String getLocale()                   { return locale; }
    public void   setLocale(String locale)      { this.locale = locale; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OfferCarouselSettingsTranslationId that)) return false;
        return Objects.equals(settingsId, that.settingsId)
            && Objects.equals(locale,     that.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(settingsId, locale);
    }
}
