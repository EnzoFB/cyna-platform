package com.cyna.modules.product.domain.model;

import com.cyna.shared.domain.Guard;

import java.time.Instant;
import java.util.UUID;

public class ProductImage {

    private final UUID id;
    private final UUID productId;
    private final byte[] imageData;
    private final String mimeType;
    private final int displayOrder;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ProductImage(UUID id,
                         UUID productId,
                         byte[] imageData,
                         String mimeType,
                         int displayOrder,
                         Instant createdAt,
                         Instant updatedAt) {
        this.id = id;
        this.productId = productId;
        this.imageData = imageData;
        this.mimeType = mimeType;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductImage create(UUID productId,
                                      byte[] imageData,
                                      String mimeType,
                                      int displayOrder) {
        Guard.againstNull(productId, "productId");
        Guard.againstNull(imageData, "imageData");
        Guard.againstNullOrBlank(mimeType, "mimeType");
        if (displayOrder < 0) throw new IllegalArgumentException("displayOrder must be >= 0");
        Instant now = Instant.now();
        return new ProductImage(UUID.randomUUID(), productId, imageData, mimeType, displayOrder, now, now);
    }

    public static ProductImage reconstitute(UUID id,
                                            UUID productId,
                                            byte[] imageData,
                                            String mimeType,
                                            int displayOrder,
                                            Instant createdAt,
                                            Instant updatedAt) {
        Guard.againstNull(id, "id");
        Guard.againstNull(productId, "productId");
        Guard.againstNull(imageData, "imageData");
        Guard.againstNullOrBlank(mimeType, "mimeType");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");
        if (displayOrder < 0) throw new IllegalArgumentException("displayOrder must be >= 0");
        return new ProductImage(id, productId, imageData, mimeType, displayOrder, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public byte[] getImageData() { return imageData; }
    public String getMimeType() { return mimeType; }
    public int getDisplayOrder() { return displayOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
