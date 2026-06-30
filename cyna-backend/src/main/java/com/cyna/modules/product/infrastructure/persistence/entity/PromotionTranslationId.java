package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PromotionTranslationId implements Serializable {

    @Column(name = "promotion_id", nullable = false)
    private UUID promotionId;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    public PromotionTranslationId() {}

    public PromotionTranslationId(UUID promotionId, String locale) {
        this.promotionId = promotionId;
        this.locale      = locale;
    }

    public UUID   getPromotionId()              { return promotionId; }
    public void   setPromotionId(UUID promotionId) { this.promotionId = promotionId; }
    public String getLocale()                   { return locale; }
    public void   setLocale(String locale)      { this.locale = locale; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PromotionTranslationId that)) return false;
        return Objects.equals(promotionId, that.promotionId)
            && Objects.equals(locale,      that.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(promotionId, locale);
    }
}
