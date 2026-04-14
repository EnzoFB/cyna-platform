package com.cyna.modules.product.domain.model;

import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Money;

import java.time.Instant;
import java.util.UUID;

public class Product extends AggregateRoot<UUID> {

    private final String name;
    private final UUID categoryId;
    private final int priorityLevel;
    private final String serviceDescription;
    private final String technicalDescription;
    private final Money monthlyPrice;
    private final Money annualPrice;
    private final boolean isPublished;
    private final boolean isAvailable;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Product(UUID id,
                    String name,
                    UUID categoryId,
                    int priorityLevel,
                    String serviceDescription,
                    String technicalDescription,
                    Money monthlyPrice,
                    Money annualPrice,
                    boolean isPublished,
                    boolean isAvailable,
                    Instant createdAt,
                    Instant updatedAt) {
        super(id);
        this.name = name;
        this.categoryId = categoryId;
        this.priorityLevel = priorityLevel;
        this.serviceDescription = serviceDescription;
        this.technicalDescription = technicalDescription;
        this.monthlyPrice = monthlyPrice;
        this.annualPrice = annualPrice;
        this.isPublished = isPublished;
        this.isAvailable = isAvailable;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product create(String name,
                                 UUID categoryId,
                                 int priorityLevel,
                                 String serviceDescription,
                                 String technicalDescription,
                                 Money monthlyPrice,
                                 Money annualPrice) {
        Guard.againstNullOrBlank(name, "name");
        Guard.againstNull(categoryId, "categoryId");
        Guard.againstNullOrBlank(serviceDescription, "serviceDescription");
        Guard.againstNullOrBlank(technicalDescription, "technicalDescription");
        Guard.againstNull(monthlyPrice, "monthlyPrice");
        Guard.againstNull(annualPrice, "annualPrice");

        Instant now = Instant.now();
        return new Product(
            UUID.randomUUID(),
            name,
            categoryId,
            priorityLevel,
            serviceDescription,
            technicalDescription,
            monthlyPrice,
            annualPrice,
            false,
            true,
            now,
            now
        );
    }

    public static Product reconstitute(UUID id,
                                       String name,
                                       UUID categoryId,
                                       int priorityLevel,
                                       String serviceDescription,
                                       String technicalDescription,
                                       Money monthlyPrice,
                                       Money annualPrice,
                                       boolean isPublished,
                                       boolean isAvailable,
                                       Instant createdAt,
                                       Instant updatedAt) {
        Guard.againstNull(id, "id");
        Guard.againstNullOrBlank(name, "name");
        Guard.againstNull(categoryId, "categoryId");
        Guard.againstNullOrBlank(serviceDescription, "serviceDescription");
        Guard.againstNullOrBlank(technicalDescription, "technicalDescription");
        Guard.againstNull(monthlyPrice, "monthlyPrice");
        Guard.againstNull(annualPrice, "annualPrice");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");

        return new Product(
            id,
            name,
            categoryId,
            priorityLevel,
            serviceDescription,
            technicalDescription,
            monthlyPrice,
            annualPrice,
            isPublished,
            isAvailable,
            createdAt,
            updatedAt
        );
    }

    public String getName() {
        return name;
    }

    public UUID getCategoryId() { return categoryId; }

    public int getPriorityLevel() { return priorityLevel; }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public String getTechnicalDescription() {
        return technicalDescription;
    }

    public Money getMonthlyPrice() {
        return monthlyPrice;
    }

    public Money getAnnualPrice() {
        return annualPrice;
    }

    public boolean isPublished() { return isPublished; }

    public boolean isAvailable() { return isAvailable; }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
