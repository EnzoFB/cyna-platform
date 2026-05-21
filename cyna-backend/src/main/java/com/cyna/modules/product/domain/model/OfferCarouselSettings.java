package com.cyna.modules.product.domain.model;

import com.cyna.shared.domain.Guard;

import java.time.Instant;
import java.util.Map;

public class OfferCarouselSettings {

    private final Map<String, CarouselSettingsTranslation> translations;
    private final Instant createdAt;
    private final Instant updatedAt;

    private OfferCarouselSettings(Map<String, CarouselSettingsTranslation> translations,
                                  Instant createdAt,
                                  Instant updatedAt) {
        this.translations = translations != null ? Map.copyOf(translations) : Map.of();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OfferCarouselSettings createDefault() {
        Instant now = Instant.now();
        return new OfferCarouselSettings(Map.of(), now, now);
    }

    public static OfferCarouselSettings reconstitute(Map<String, CarouselSettingsTranslation> translations,
                                                     Instant createdAt,
                                                     Instant updatedAt) {
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");
        return new OfferCarouselSettings(translations, createdAt, updatedAt);
    }

    public OfferCarouselSettings updateTranslations(Map<String, CarouselSettingsTranslation> translations) {
        return new OfferCarouselSettings(normalize(translations), createdAt, Instant.now());
    }

    /** Returns the fixed text for the given locale, falling back to "fr". */
    public String getFixedText(String locale) {
        CarouselSettingsTranslation t = translations.get(locale);
        if (t == null) t = translations.get("fr");
        return t != null ? t.fixedText() : "";
    }

    public Map<String, CarouselSettingsTranslation> getTranslations() { return translations; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private static Map<String, CarouselSettingsTranslation> normalize(Map<String, CarouselSettingsTranslation> t) {
        if (t == null) return Map.of();
        var builder = new java.util.HashMap<String, CarouselSettingsTranslation>();
        t.forEach((locale, tr) -> builder.put(locale,
                new CarouselSettingsTranslation(tr.fixedText() != null ? tr.fixedText().trim() : "")));
        return Map.copyOf(builder);
    }
}
