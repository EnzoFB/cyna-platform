package com.cyna.modules.product.domain.repository;

import com.cyna.modules.product.domain.model.OfferCarouselSettings;

import java.util.Optional;

public interface OfferCarouselSettingsRepository {

    Optional<OfferCarouselSettings> find();

    void save(OfferCarouselSettings settings);
}
