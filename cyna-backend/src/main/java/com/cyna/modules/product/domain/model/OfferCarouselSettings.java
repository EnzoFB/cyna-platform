package com.cyna.modules.product.domain.model;

import com.cyna.shared.domain.Guard;

import java.time.Instant;

public class OfferCarouselSettings {

    private final String fixedTextFr;
    private final String fixedTextEn;
    private final Instant createdAt;
    private final Instant updatedAt;

    private OfferCarouselSettings(String fixedTextFr,
                                  String fixedTextEn,
                                  Instant createdAt,
                                  Instant updatedAt) {
        this.fixedTextFr = fixedTextFr;
        this.fixedTextEn = fixedTextEn;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OfferCarouselSettings createDefault() {
        Instant now = Instant.now();
        return new OfferCarouselSettings("", "", now, now);
    }

    public static OfferCarouselSettings reconstitute(String fixedTextFr,
                                                     String fixedTextEn,
                                                     Instant createdAt,
                                                     Instant updatedAt) {
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");
        return new OfferCarouselSettings(
                normalize(fixedTextFr),
                normalize(fixedTextEn),
                createdAt,
                updatedAt
        );
    }

    public OfferCarouselSettings updateTexts(String fixedTextFr, String fixedTextEn) {
        return new OfferCarouselSettings(
                normalize(fixedTextFr),
                normalize(fixedTextEn),
                createdAt,
                Instant.now()
        );
    }

    public String getFixedTextFr() {
        return fixedTextFr;
    }

    public String getFixedTextEn() {
        return fixedTextEn;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
