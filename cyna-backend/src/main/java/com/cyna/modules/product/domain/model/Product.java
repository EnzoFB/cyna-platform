package com.cyna.modules.product.domain.model;

import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Money;

import java.time.Instant;
import java.util.UUID;

public class Product extends AggregateRoot<UUID> {

    private final String name;
    private final ProductCategory category;
    private final String serviceDescription;
    private final String technicalDescription;
    private final Money monthlyPrice;
    private final Money annualPrice;
    private final ProductStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Product(UUID id,
                    String name,
                    ProductCategory category,
                    String serviceDescription,
                    String technicalDescription,
                    Money monthlyPrice,
                    Money annualPrice,
                    ProductStatus status,
                    Instant createdAt,
                    Instant updatedAt) {
        super(id);
        this.name = name;
        this.category = category;
        this.serviceDescription = serviceDescription;
        this.technicalDescription = technicalDescription;
        this.monthlyPrice = monthlyPrice;
        this.annualPrice = annualPrice;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product create(String name,
                                 ProductCategory category,
                                 String serviceDescription,
                                 String technicalDescription,
                                 Money monthlyPrice,
                                 Money annualPrice) {
        Guard.againstNullOrBlank(name, "name");
        Guard.againstNull(category, "category");
        Guard.againstNullOrBlank(serviceDescription, "serviceDescription");
        Guard.againstNullOrBlank(technicalDescription, "technicalDescription");
        Guard.againstNull(monthlyPrice, "monthlyPrice");
        Guard.againstNull(annualPrice, "annualPrice");

        if (!monthlyPrice.currency().equals(annualPrice.currency())) {
            throw new IllegalArgumentException("monthlyPrice and annualPrice must have the same currency");
        }

        Instant now = Instant.now();
        return new Product(
                UUID.randomUUID(),
                name,
                category,
                serviceDescription,
                technicalDescription,
                monthlyPrice,
                annualPrice,
                ProductStatus.DRAFT,
                now,
                now
        );
    }

    public static Product reconstitute(UUID id,
                                       String name,
                                       ProductCategory category,
                                       String serviceDescription,
                                       String technicalDescription,
                                       Money monthlyPrice,
                                       Money annualPrice,
                                       ProductStatus status,
                                       Instant createdAt,
                                       Instant updatedAt) {
        Guard.againstNull(id, "id");
        Guard.againstNullOrBlank(name, "name");
        Guard.againstNull(category, "category");
        Guard.againstNullOrBlank(serviceDescription, "serviceDescription");
        Guard.againstNullOrBlank(technicalDescription, "technicalDescription");
        Guard.againstNull(monthlyPrice, "monthlyPrice");
        Guard.againstNull(annualPrice, "annualPrice");
        Guard.againstNull(status, "status");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");

        if (!monthlyPrice.currency().equals(annualPrice.currency())) {
            throw new IllegalArgumentException("monthlyPrice and annualPrice must have the same currency");
        }

        return new Product(
                id,
                name,
                category,
                serviceDescription,
                technicalDescription,
                monthlyPrice,
                annualPrice,
                status,
                createdAt,
                updatedAt
        );
    }

    public String getName() {
        return name;
    }

    public ProductCategory getCategory() {
        return category;
    }

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

    public ProductStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
