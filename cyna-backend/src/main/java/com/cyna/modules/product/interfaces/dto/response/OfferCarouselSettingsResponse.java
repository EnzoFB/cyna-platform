package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.getoffercarouselsettings.OfferCarouselSettingsReadModel;
import com.cyna.modules.product.application.translation.CarouselSettingsTranslationDto;

import java.util.Map;

public record OfferCarouselSettingsResponse(
        Map<String, CarouselSettingsTranslationDto> translations,
        int maxSlides
) {
    public static OfferCarouselSettingsResponse from(OfferCarouselSettingsReadModel model) {
        return new OfferCarouselSettingsResponse(model.translations(), model.maxSlides());
    }
}
