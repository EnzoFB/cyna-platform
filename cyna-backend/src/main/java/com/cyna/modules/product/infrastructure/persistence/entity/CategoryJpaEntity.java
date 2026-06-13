package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "categories", schema = "product_schema")
public class CategoryJpaEntity {

    @Id
    private UUID id;

    /** Technical slug — unique, never translated (used for image paths etc.). */
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "image")
    private byte[] image;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** All locale translations (fr, en, …). */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<CategoryTranslationJpaEntity> translations = new HashSet<>();

    public CategoryJpaEntity() {}

    public void setTranslations(Set<CategoryTranslationJpaEntity> translations) {
        this.translations.clear();
        if (translations != null) {
            this.translations.addAll(translations);
        }
    }
}
