package com.cyna.modules.product.domain.model;

import com.cyna.modules.product.domain.event.CategoryCreated;
import com.cyna.modules.product.domain.event.CategoryDeleted;
import com.cyna.modules.product.domain.event.CategoryUpdated;
import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Result;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class Category extends AggregateRoot<UUID> {

    private final String name;
    private final Map<String, CategoryTranslation> translations;
    private final byte[] image;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Category(UUID id,
                     String name,
                     Map<String, CategoryTranslation> translations,
                     byte[] image,
                     boolean active,
                     Instant createdAt,
                     Instant updatedAt) {
        super(id);
        this.name = name;
        this.translations = Map.copyOf(translations);
        this.image = image;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Category create(String name, Map<String, CategoryTranslation> translations, byte[] image) {
        Guard.againstNullOrBlank(name, "name");
        validateTranslations(translations);

        var now = Instant.now();
        var category = new Category(UUID.randomUUID(), name, translations, image, true, now, now);
        category.raise(new CategoryCreated(category.getId(), name, now));
        return category;
    }

    public Result<Category> update(String name, Map<String, CategoryTranslation> translations, byte[] image) {
        Guard.againstNullOrBlank(name, "name");
        validateTranslations(translations);

        var now = Instant.now();
        var updated = new Category(
                this.getId(),
                name,
                translations,
                image != null ? image : this.image,
                this.active,
                this.createdAt,
                now
        );

        updated.raise(new CategoryUpdated(
                this.getId(),
                name,
                translations.getOrDefault("fr", new CategoryTranslation("", "")).description(),
                now
        ));

        return Result.success(updated);
    }

    public Result<Category> deactivate() {
        if (!this.active) {
            return Result.failure("Category is already inactive");
        }

        var now = Instant.now();
        var deactivated = new Category(this.getId(), this.name, this.translations, this.image, false, this.createdAt, now);
        deactivated.raise(new CategoryDeleted(this.getId(), now));
        return Result.success(deactivated);
    }

    public Result<Category> activate() {
        if (this.active) {
            return Result.failure("Category is already active");
        }

        var now = Instant.now();
        var activated = new Category(this.getId(), this.name, this.translations, this.image, true, this.createdAt, now);
        return Result.success(activated);
    }

    public static Category reconstitute(UUID id,
                                        String name,
                                        Map<String, CategoryTranslation> translations,
                                        byte[] image,
                                        boolean active,
                                        Instant createdAt,
                                        Instant updatedAt) {
        Guard.againstNull(id, "id");
        Guard.againstNullOrBlank(name, "name");
        validateTranslations(translations);
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");

        return new Category(id, name, translations, image, active, createdAt, updatedAt);
    }

    private static void validateTranslations(Map<String, CategoryTranslation> translations) {
        Guard.againstNull(translations, "translations");
        CategoryTranslation fr = translations.get("fr");
        Guard.againstNull(fr, "translations[fr]");
        Guard.againstNullOrBlank(fr.fullName(), "translations[fr].fullName");
    }

    public String getName() { return name; }
    public Map<String, CategoryTranslation> getTranslations() { return translations; }

    /** Convenience accessor — returns the FR (primary) full name. */
    public String getFullName() {
        CategoryTranslation fr = translations.get("fr");
        return fr != null ? fr.fullName() : "";
    }

    public byte[] getImage() { return image; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
