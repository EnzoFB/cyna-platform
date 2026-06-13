package com.cyna.modules.product.application.query.getoffercarouselsettings;

import com.cyna.modules.product.domain.model.CarouselSettingsTranslation;

import java.util.Map;

public record OfferCarouselSettingsReadModel(
        Map<String, CarouselSettingsTranslation> translations,
        int maxSlides
) {}
