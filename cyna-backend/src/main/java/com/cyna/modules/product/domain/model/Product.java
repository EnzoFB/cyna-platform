package com.cyna.modules.product.domain.model;

import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.Guard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class Product extends AggregateRoot<UUID> {

    private final Map<String, ProductTranslation> translations;
    private final UUID categoryId;
    private final int priorityLevel;
    private final BigDecimal monthlyPrice;
    private final BigDecimal annualPrice;
    private final String currency;
    private final boolean isPublished;
    private final boolean isAvailable;
    private final int freeTrialDays;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Product(UUID id,
                    Map<String, ProductTranslation> translations,
                    UUID categoryId,
                    int priorityLevel,
                    BigDecimal monthlyPrice,
                    BigDecimal annualPrice,
                    String currency,
                    boolean isPublished,
                    boolean isAvailable,
                    int freeTrialDays,
                    Instant createdAt,
                    Instant updatedAt) {
        super(id);
        this.translations = Map.copyOf(translations);
        this.categoryId = categoryId;
        this.priorityLevel = priorityLevel;
        this.monthlyPrice = monthlyPrice;
        this.annualPrice = annualPrice;
        this.currency = currency;
        this.isPublished = isPublished;
        this.isAvailable = isAvailable;
        this.freeTrialDays = freeTrialDays;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product create(Map<String, ProductTranslation> translations,
                                 UUID categoryId,
                                 int priorityLevel,
                                 BigDecimal monthlyPrice,
                                 BigDecimal annualPrice,
                                 String currency,
                                 int freeTrialDays) {
        validateTranslations(translations);
        Guard.againstNull(categoryId, "categoryId");
        Guard.againstNull(monthlyPrice, "monthlyPrice");
        Guard.againstNull(annualPrice, "annualPrice");
        Guard.againstNullOrBlank(currency, "currency");

        if (monthlyPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("monthlyPrice must not be negative");
        }
        if (annualPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("annualPrice must not be negative");
        }

        Instant now = Instant.now();
        return new Product(
                UUID.randomUUID(),
                translations,
                categoryId,
                priorityLevel,
                monthlyPrice,
                annualPrice,
                currency,
                false,
                true,
                freeTrialDays,
                now,
                now
        );
    }

    public static Product reconstitute(UUID id,
                                       Map<String, ProductTranslation> translations,
                                       UUID categoryId,
                                       int priorityLevel,
                                       BigDecimal monthlyPrice,
                                       BigDecimal annualPrice,
                                       String currency,
                                       boolean isPublished,
                                       boolean isAvailable,
                                       int freeTrialDays,
                                       Instant createdAt,
                                       Instant updatedAt) {
        Guard.againstNull(id, "id");
        validateTranslations(translations);
        Guard.againstNull(categoryId, "categoryId");
        Guard.againstNull(monthlyPrice, "monthlyPrice");
        Guard.againstNull(annualPrice, "annualPrice");
        Guard.againstNullOrBlank(currency, "currency");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");

        return new Product(
                id,
                translations,
                categoryId,
                priorityLevel,
                monthlyPrice,
                annualPrice,
                currency,
                isPublished,
                isAvailable,
                freeTrialDays,
                createdAt,
                updatedAt
        );
    }

    private static void validateTranslations(Map<String, ProductTranslation> translations) {
        Guard.againstNull(translations, "translations");
        ProductTranslation fr = translations.get("fr");
        Guard.againstNull(fr, "translations[fr]");
        Guard.againstNullOrBlank(fr.name(), "translations[fr].name");
        Guard.againstNullOrBlank(fr.serviceDescription(), "translations[fr].serviceDescription");
        Guard.againstNullOrBlank(fr.technicalDescription(), "translations[fr].technicalDescription");
    }

    public Map<String, ProductTranslation> getTranslations() { return translations; }

    /** Convenience accessor — returns the FR (primary) name. */
    public String getName() {
        ProductTranslation fr = translations.get("fr");
        return fr != null ? fr.name() : "";
    }

    public UUID getCategoryId() { return categoryId; }
    public int getPriorityLevel() { return priorityLevel; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public BigDecimal getAnnualPrice() { return annualPrice; }
    public String getCurrency() { return currency; }
    public boolean isPublished() { return isPublished; }
    public boolean isAvailable() { return isAvailable; }
    public int getFreeTrialDays() { return freeTrialDays; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
