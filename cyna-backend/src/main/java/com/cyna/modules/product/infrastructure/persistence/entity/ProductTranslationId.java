package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ProductTranslationId implements Serializable {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    public ProductTranslationId() {}

    public ProductTranslationId(UUID productId, String locale) {
        this.productId = productId;
        this.locale    = locale;
    }

    public UUID   getProductId() { return productId; }
    public void   setProductId(UUID productId) { this.productId = productId; }
    public String getLocale()    { return locale; }
    public void   setLocale(String locale) { this.locale = locale; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductTranslationId that)) return false;
        return Objects.equals(productId, that.productId)
            && Objects.equals(locale,    that.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, locale);
    }
}
