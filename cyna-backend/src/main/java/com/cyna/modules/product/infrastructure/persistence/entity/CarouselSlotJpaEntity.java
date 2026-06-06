package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "carousel_slots", schema = "product_schema")
public class CarouselSlotJpaEntity {

    @Id
    private UUID id;

    @Column(name = "promotion_id", nullable = false, unique = true)
    private UUID promotionId;

    @Column(name = "slot_order", nullable = false, unique = true)
    private int slotOrder;

    public CarouselSlotJpaEntity() {}

    public UUID getId()                    { return id; }
    public void setId(UUID id)             { this.id = id; }

    public UUID getPromotionId()           { return promotionId; }
    public void setPromotionId(UUID v)     { this.promotionId = v; }

    public int getSlotOrder()              { return slotOrder; }
    public void setSlotOrder(int v)        { this.slotOrder = v; }
}
