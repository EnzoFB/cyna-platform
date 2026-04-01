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
    private final String description;
    private final byte[] image;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Category(UUID id, String name, String description, byte[] image,
                     boolean active, Instant createdAt, Instant updatedAt) {
        super(id);
        this.name = name;
        this.description = description;
        this.image = image;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Category create(String name, String description, byte[] image) {
        Guard.againstNullOrBlank(name, "name");
        Guard.againstNull(description, "description");

        var now = Instant.now();
        var category = new Category(
                UUID.randomUUID(), name, description, image, true, now, now
        );

        category.raise(new CategoryCreated(category.getId(), name, now));

        return category;
    }

    public Result<Category> update(String name, String description, byte[] image) {
        Guard.againstNullOrBlank(name, "name");
        Guard.againstNull(description, "description");

        var now = Instant.now();
        var updated = new Category(
                this.getId(), name, description, image != null ? image : this.image, this.active, this.createdAt, now
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
                this.getId(), this.name, this.description, this.image, false, this.createdAt, now
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
                this.getId(), this.name, this.description, this.image, true, this.createdAt, now
        );

        return Result.success(activated);
    }

    public static Category reconstitute(UUID id, String name, String description,
                                        byte[] image, boolean active,
                                        Instant createdAt, Instant updatedAt) {
        return new Category(id, name, description, image, active, createdAt, updatedAt);
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public byte[] getImage() { return image; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
