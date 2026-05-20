package com.cyna.modules.product.domain.model;

import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.Guard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Product extends AggregateRoot<UUID> {

    private final String name;
    private final String nameEn;
    private final UUID categoryId;
    private final int priorityLevel;
    private final String serviceDescription;
    private final String serviceDescriptionEn;
    private final String technicalDescription;
    private final String technicalDescriptionEn;
    private final BigDecimal monthlyPrice;
    private final BigDecimal annualPrice;
    private final String currency;
    private final boolean isPublished;
    private final boolean isAvailable;
    private final int freeTrialDays;
    private final List<String> highlightPoints;
    private final List<String> highlightPointsEn;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Product(UUID id,
                    String name,
                    String nameEn,
                    UUID categoryId,
                    int priorityLevel,
                    String serviceDescription,
                    String serviceDescriptionEn,
                    String technicalDescription,
                    String technicalDescriptionEn,
                    BigDecimal monthlyPrice,
                    BigDecimal annualPrice,
                    String currency,
                    boolean isPublished,
                    boolean isAvailable,
                    int freeTrialDays,
                    List<String> highlightPoints,
                    List<String> highlightPointsEn,
                    Instant createdAt,
                    Instant updatedAt) {
        super(id);
        this.name = name;
        this.nameEn = nameEn != null ? nameEn : "";
        this.categoryId = categoryId;
        this.priorityLevel = priorityLevel;
        this.serviceDescription = serviceDescription;
        this.serviceDescriptionEn = serviceDescriptionEn != null ? serviceDescriptionEn : "";
        this.technicalDescription = technicalDescription;
        this.technicalDescriptionEn = technicalDescriptionEn != null ? technicalDescriptionEn : "";
        this.monthlyPrice = monthlyPrice;
        this.annualPrice = annualPrice;
        this.currency = currency;
        this.isPublished = isPublished;
        this.isAvailable = isAvailable;
        this.freeTrialDays = freeTrialDays;
        this.highlightPoints = highlightPoints != null ? List.copyOf(highlightPoints) : List.of();
        this.highlightPointsEn = highlightPointsEn != null ? List.copyOf(highlightPointsEn) : List.of();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product create(String name,
                                 String nameEn,
                                 UUID categoryId,
                                 int priorityLevel,
                                 String serviceDescription,
                                 String serviceDescriptionEn,
                                 String technicalDescription,
                                 String technicalDescriptionEn,
                                 BigDecimal monthlyPrice,
                                 BigDecimal annualPrice,
                                 String currency,
                                 int freeTrialDays,
                                 List<String> highlightPoints,
                                 List<String> highlightPointsEn) {
        Guard.againstNullOrBlank(name, "name");
        Guard.againstNull(categoryId, "categoryId");
        Guard.againstNullOrBlank(serviceDescription, "serviceDescription");
        Guard.againstNullOrBlank(technicalDescription, "technicalDescription");
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
            name,
            nameEn,
            categoryId,
            priorityLevel,
            serviceDescription,
            serviceDescriptionEn,
            technicalDescription,
            technicalDescriptionEn,
            monthlyPrice,
            annualPrice,
            currency,
            false,
            true,
            freeTrialDays,
            highlightPoints,
            highlightPointsEn,
            now,
            now
        );
    }

    public static Product reconstitute(UUID id,
                                       String name,
                                       String nameEn,
                                       UUID categoryId,
                                       int priorityLevel,
                                       String serviceDescription,
                                       String serviceDescriptionEn,
                                       String technicalDescription,
                                       String technicalDescriptionEn,
                                       BigDecimal monthlyPrice,
                                       BigDecimal annualPrice,
                                       String currency,
                                       boolean isPublished,
                                       boolean isAvailable,
                                       int freeTrialDays,
                                       List<String> highlightPoints,
                                       List<String> highlightPointsEn,
                                       Instant createdAt,
                                       Instant updatedAt) {
        Guard.againstNull(id, "id");
        Guard.againstNullOrBlank(name, "name");
        Guard.againstNull(categoryId, "categoryId");
        Guard.againstNullOrBlank(serviceDescription, "serviceDescription");
        Guard.againstNullOrBlank(technicalDescription, "technicalDescription");
        Guard.againstNull(monthlyPrice, "monthlyPrice");
        Guard.againstNull(annualPrice, "annualPrice");
        Guard.againstNullOrBlank(currency, "currency");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");

        return new Product(
            id,
            name,
            nameEn,
            categoryId,
            priorityLevel,
            serviceDescription,
            serviceDescriptionEn,
            technicalDescription,
            technicalDescriptionEn,
            monthlyPrice,
            annualPrice,
            currency,
            isPublished,
            isAvailable,
            freeTrialDays,
            highlightPoints,
            highlightPointsEn,
            createdAt,
            updatedAt
        );
    }

    public String getName() { return name; }
    public String getNameEn() { return nameEn; }
    public UUID getCategoryId() { return categoryId; }
    public int getPriorityLevel() { return priorityLevel; }
    public String getServiceDescription() { return serviceDescription; }
    public String getServiceDescriptionEn() { return serviceDescriptionEn; }
    public String getTechnicalDescription() { return technicalDescription; }
    public String getTechnicalDescriptionEn() { return technicalDescriptionEn; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public BigDecimal getAnnualPrice() { return annualPrice; }
    public String getCurrency() { return currency; }
    public boolean isPublished() { return isPublished; }
    public boolean isAvailable() { return isAvailable; }
    public int getFreeTrialDays() { return freeTrialDays; }
    public List<String> getHighlightPoints() { return highlightPoints; }
    public List<String> getHighlightPointsEn() { return highlightPointsEn; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
