package com.cyna.modules.product.domain.repository;

import com.cyna.modules.product.domain.model.CarouselSlot;

import java.util.List;
import java.util.UUID;

public interface CarouselSlotRepository {

    /** Appends the promotion to the carousel at the next available position. */
    void add(UUID promotionId);

    void remove(UUID promotionId);

    /** Returns all slots ordered by slot_order ascending. */
    List<CarouselSlot> findAll();

    int count();

    boolean existsByPromotionId(UUID promotionId);

    /** Reassigns slot_order 1..N to the given promotion IDs in the supplied order. */
    void reorder(List<UUID> orderedPromotionIds);
}
