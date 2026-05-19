package com.cyna.modules.product.domain.repository;

import com.cyna.modules.product.domain.model.Promotion;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository {

    void save(Promotion promotion);

    Optional<Promotion> findById(UUID id);

    List<Promotion> findAll();

    List<Promotion> findByProductIds(Collection<UUID> productIds);

    List<Promotion> findActiveByProductIds(Collection<UUID> productIds, Instant atInstant);

    boolean existsEnabledOverlappingWindow(UUID productId,
                                           Instant startAt,
                                           Instant endAt,
                                           UUID excludedPromotionId);

    void deleteById(UUID id);
}

