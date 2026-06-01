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
@Table(name = "promotion_translations", schema = "product_schema")
public class PromotionTranslationJpaEntity {

    @EmbeddedId
    private PromotionTranslationId id = new PromotionTranslationId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("promotionId")
    @JoinColumn(name = "promotion_id")
    private PromotionJpaEntity promotion;

    @Column(name = "marketing_text", nullable = false, columnDefinition = "text")
    private String marketingText;

    public PromotionTranslationJpaEntity() {}

    // ── Convenience factory ───────────────────────────────────────────────────

    public static PromotionTranslationJpaEntity of(PromotionJpaEntity promotion,
                                                    String locale,
                                                    String marketingText) {
        var t = new PromotionTranslationJpaEntity();
        t.id            = new PromotionTranslationId(promotion.getId(), locale);
        t.promotion     = promotion;
        t.marketingText = marketingText;
        return t;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public PromotionTranslationId getId()                      { return id; }
    public void setId(PromotionTranslationId id)               { this.id = id; }

    public String getLocale()                                  { return id.getLocale(); }

    public PromotionJpaEntity getPromotion()                   { return promotion; }
    public void setPromotion(PromotionJpaEntity promotion)     { this.promotion = promotion; }

    public String getMarketingText()                           { return marketingText; }
    public void setMarketingText(String marketingText)         { this.marketingText = marketingText; }
}
