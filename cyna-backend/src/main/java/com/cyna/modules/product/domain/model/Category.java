package com.cyna.modules.product.domain.model;

import com.cyna.modules.product.domain.event.CategoryCreated;
import com.cyna.modules.product.domain.event.CategoryDeleted;
import com.cyna.modules.product.domain.event.CategoryUpdated;
import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Result;

import java.time.Instant;
import java.util.UUID;

public class Category extends AggregateRoot<UUID> {

    private final String name;
    private final String fullName;
    private final String fullNameEn;
    private final String description;
    private final String descriptionEn;
    private final byte[] image;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Category(UUID id,
                     String name,
                     String fullName,
                     String fullNameEn,
                     String description,
                     String descriptionEn,
                     byte[] image,
                     boolean active,
                     Instant createdAt,
                     Instant updatedAt) {
        super(id);
        this.name = name;
        this.fullName = fullName;
        this.fullNameEn = fullNameEn != null ? fullNameEn : "";
        this.description = description;
        this.descriptionEn = descriptionEn != null ? descriptionEn : "";
        this.image = image;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Category create(String name, String fullName, String fullNameEn,
                                  String description, String descriptionEn, byte[] image) {
        Guard.againstNullOrBlank(name, "name");
        Guard.againstNullOrBlank(fullName, "fullName");
        Guard.againstNull(description, "description");

        var now = Instant.now();

        var category = new Category(
            UUID.randomUUID(),
            name,
            fullName,
            fullNameEn,
            description,
            descriptionEn,
            image,
            true,
            now,
            now
        );

        category.raise(new CategoryCreated(category.getId(), name, now));

        return category;
    }

    public Result<Category> update(String name, String fullName, String fullNameEn,
                                   String description, String descriptionEn, byte[] image) {
        Guard.againstNullOrBlank(name, "name");
        Guard.againstNullOrBlank(fullName, "fullName");
        Guard.againstNull(description, "description");

        var now = Instant.now();

        var updated = new Category(
            this.getId(),
            name,
            fullName,
            fullNameEn,
            description,
            descriptionEn,
            image != null ? image : this.image,
            this.active,
            this.createdAt,
            now
        );

        updated.raise(new CategoryUpdated(this.getId(), name, description, now));

        return Result.success(updated);
    }

    public Result<Category> deactivate() {
        if (!this.active) {
            return Result.failure("Category is already inactive");
        }

        var now = Instant.now();
        var deactivated = new Category(
                this.getId(), this.name, this.fullName, this.fullNameEn,
                this.description, this.descriptionEn, this.image, false, this.createdAt, now
        );

        deactivated.raise(new CategoryDeleted(this.getId(), now));

        return Result.success(deactivated);
    }

    public Result<Category> activate() {
        if (this.active) {
            return Result.failure("Category is already active");
        }

        var now = Instant.now();
        var activated = new Category(
                this.getId(), this.name, this.fullName, this.fullNameEn,
                this.description, this.descriptionEn, this.image, true, this.createdAt, now
        );

        return Result.success(activated);
    }

    public static Category reconstitute(UUID id,
                                        String name,
                                        String fullName,
                                        String fullNameEn,
                                        String description,
                                        String descriptionEn,
                                        byte[] image,
                                        boolean active,
                                        Instant createdAt,
                                        Instant updatedAt) {

        Guard.againstNull(id, "id");
        Guard.againstNullOrBlank(name, "name");
        Guard.againstNullOrBlank(fullName, "fullName");
        Guard.againstNull(description, "description");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");

        return new Category(
            id,
            name,
            fullName,
            fullNameEn,
            description,
            descriptionEn,
            image,
            active,
            createdAt,
            updatedAt
        );
    }

    public String getName() { return name; }
    public String getFullName() { return fullName; }
    public String getFullNameEn() { return fullNameEn; }
    public String getDescription() { return description; }
    public String getDescriptionEn() { return descriptionEn; }
    public byte[] getImage() { return image; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
