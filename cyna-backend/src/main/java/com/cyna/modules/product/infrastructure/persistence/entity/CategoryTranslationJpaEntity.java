package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "category_translations", schema = "product_schema")
public class CategoryTranslationJpaEntity {

    @EmbeddedId
    private CategoryTranslationId id = new CategoryTranslationId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private CategoryJpaEntity category;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    public CategoryTranslationJpaEntity() {}

    // ── Convenience factory ───────────────────────────────────────────────────

    public static CategoryTranslationJpaEntity of(CategoryJpaEntity category,
                                                   String locale,
                                                   String fullName,
                                                   String description) {
        var t = new CategoryTranslationJpaEntity();
        t.id          = new CategoryTranslationId(category.getId(), locale);
        t.category    = category;
        t.fullName    = fullName;
        t.description = description;
        return t;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public CategoryTranslationId getId()  { return id; }
    public void setId(CategoryTranslationId id) { this.id = id; }

    public String getLocale() { return id.getLocale(); }

    public CategoryJpaEntity getCategory()  { return category; }
    public void setCategory(CategoryJpaEntity category) { this.category = category; }

    public String getFullName()    { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
