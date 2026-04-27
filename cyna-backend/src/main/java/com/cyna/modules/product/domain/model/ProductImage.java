package com.cyna.modules.product.domain.model;

import com.cyna.shared.domain.Guard;

import java.time.Instant;
import java.util.UUID;

public class ProductImage {

    private final UUID id;
    private final UUID productId;
    private final String imageUrl;
    private final int displayOrder;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ProductImage(UUID id,
                         UUID productId,
                         String imageUrl,
                         int displayOrder,
                         Instant createdAt,
                         Instant updatedAt) {
        this.id = id;
        this.productId = productId;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductImage reconstitute(UUID id,
                                            UUID productId,
                                            String imageUrl,
                                            int displayOrder,
                                            Instant createdAt,
                                            Instant updatedAt) {
        Guard.againstNull(id, "id");
        Guard.againstNull(productId, "productId");
        Guard.againstNullOrBlank(imageUrl, "imageUrl");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder must be >= 0");
        }

        return new ProductImage(id, productId, imageUrl, displayOrder, createdAt, updatedAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

