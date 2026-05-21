package com.cyna.modules.product.domain.model;

import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.Guard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class Promotion extends AggregateRoot<UUID> {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final UUID productId;
    private final int discountPercent;
    private final Map<String, PromotionTranslation> translations;
    private final Instant startAt;
    private final Instant endAt;
    private final boolean enabled;
    private final boolean showInCarousel;
    private final Integer carouselOrder;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Promotion(UUID id,
                      UUID productId,
                      int discountPercent,
                      Map<String, PromotionTranslation> translations,
                      Instant startAt,
                      Instant endAt,
                      boolean enabled,
                      boolean showInCarousel,
                      Integer carouselOrder,
                      Instant createdAt,
                      Instant updatedAt) {
        super(id);
        this.productId = productId;
        this.discountPercent = discountPercent;
        this.translations = Map.copyOf(translations);
        this.startAt = startAt;
        this.endAt = endAt;
        this.enabled = enabled;
        this.showInCarousel = showInCarousel;
        this.carouselOrder = carouselOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Promotion create(UUID productId,
                                   int discountPercent,
                                   Map<String, PromotionTranslation> translations,
                                   Instant startAt,
                                   Instant endAt,
                                   boolean enabled,
                                   boolean showInCarousel,
                                   Integer carouselOrder) {
        validate(productId, discountPercent, translations, startAt, endAt, showInCarousel, carouselOrder);
        Instant now = Instant.now();
        Integer normalizedOrder = showInCarousel ? carouselOrder : null;
        return new Promotion(
                UUID.randomUUID(),
                productId,
                discountPercent,
                normalizeTranslations(translations),
                startAt,
                endAt,
                enabled,
                showInCarousel,
                normalizedOrder,
                now,
                now
        );
    }

    public static Promotion reconstitute(UUID id,
                                         UUID productId,
                                         int discountPercent,
                                         Map<String, PromotionTranslation> translations,
                                         Instant startAt,
                                         Instant endAt,
                                         boolean enabled,
                                         boolean showInCarousel,
                                         Integer carouselOrder,
                                         Instant createdAt,
                                         Instant updatedAt) {
        Guard.againstNull(id, "id");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");
        validate(productId, discountPercent, translations, startAt, endAt, showInCarousel, carouselOrder);
        Integer normalizedOrder = showInCarousel ? carouselOrder : null;
        return new Promotion(
                id,
                productId,
                discountPercent,
                normalizeTranslations(translations),
                startAt,
                endAt,
                enabled,
                showInCarousel,
                normalizedOrder,
                createdAt,
                updatedAt
        );
    }

    public Promotion update(int discountPercent,
                            Map<String, PromotionTranslation> translations,
                            Instant startAt,
                            Instant endAt,
                            boolean enabled,
                            boolean showInCarousel,
                            Integer carouselOrder) {
        validate(productId, discountPercent, translations, startAt, endAt, showInCarousel, carouselOrder);
        Integer normalizedOrder = showInCarousel ? carouselOrder : null;
        return new Promotion(
                getId(),
                productId,
                discountPercent,
                normalizeTranslations(translations),
                startAt,
                endAt,
                enabled,
                showInCarousel,
                normalizedOrder,
                createdAt,
                Instant.now()
        );
    }

    public boolean isActiveAt(Instant instant) {
        Guard.againstNull(instant, "instant");
        return enabled && !instant.isBefore(startAt) && instant.isBefore(endAt);
    }

    public BigDecimal applyDiscount(BigDecimal amount) {
        Guard.againstNull(amount, "amount");
        if (amount.signum() <= 0) {
            return amount;
        }
        BigDecimal ratio = ONE_HUNDRED.subtract(BigDecimal.valueOf(discountPercent));
        return amount.multiply(ratio).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /** Returns the marketing text for the given locale, falling back to "fr". */
    public String getMarketingText(String locale) {
        PromotionTranslation t = translations.get(locale);
        if (t == null) t = translations.get("fr");
        return t != null ? t.marketingText() : "";
    }

    public UUID getProductId() { return productId; }
    public int getDiscountPercent() { return discountPercent; }
    public Map<String, PromotionTranslation> getTranslations() { return translations; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public boolean isEnabled() { return enabled; }
    public boolean isShowInCarousel() { return showInCarousel; }
    public Integer getCarouselOrder() { return carouselOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private static Map<String, PromotionTranslation> normalizeTranslations(Map<String, PromotionTranslation> t) {
        // Trim marketing texts
        var builder = new java.util.HashMap<String, PromotionTranslation>();
        t.forEach((locale, tr) -> builder.put(locale, new PromotionTranslation(tr.marketingText().trim())));
        return Map.copyOf(builder);
    }

    private static void validate(UUID productId,
                                 int discountPercent,
                                 Map<String, PromotionTranslation> translations,
                                 Instant startAt,
                                 Instant endAt,
                                 boolean showInCarousel,
                                 Integer carouselOrder) {
        Guard.againstNull(productId, "productId");
        Guard.againstOutOfRange(discountPercent, 1, 100, "discountPercent");
        Guard.againstNull(translations, "translations");
        PromotionTranslation fr = translations.get("fr");
        Guard.againstNull(fr, "translations[fr]");
        Guard.againstNullOrBlank(fr.marketingText(), "translations[fr].marketingText");
        Guard.againstNull(startAt, "startAt");
        Guard.againstNull(endAt, "endAt");
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("startAt must be before endAt");
        }
        if (showInCarousel) {
            Guard.againstNull(carouselOrder, "carouselOrder");
            Guard.againstOutOfRange(carouselOrder, 1, Integer.MAX_VALUE, "carouselOrder");
        }
    }
}
