package com.cyna.modules.product.application.query.getoffercarouselsettings;

import com.cyna.modules.product.application.translation.CarouselSettingsTranslationDto;

import java.util.Map;

public record OfferCarouselSettingsReadModel(
        Map<String, CarouselSettingsTranslationDto> translations,
        int maxSlides
) {}
