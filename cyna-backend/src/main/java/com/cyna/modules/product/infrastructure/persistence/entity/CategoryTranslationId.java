package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CategoryTranslationId implements Serializable {

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    public CategoryTranslationId() {}

    public CategoryTranslationId(UUID categoryId, String locale) {
        this.categoryId = categoryId;
        this.locale     = locale;
    }

    public UUID   getCategoryId() { return categoryId; }
    public void   setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public String getLocale()     { return locale; }
    public void   setLocale(String locale) { this.locale = locale; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryTranslationId that)) return false;
        return Objects.equals(categoryId, that.categoryId)
            && Objects.equals(locale,     that.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId, locale);
    }
}
