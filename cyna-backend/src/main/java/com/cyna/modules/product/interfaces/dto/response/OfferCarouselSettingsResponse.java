package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.getoffercarouselsettings.OfferCarouselSettingsReadModel;
import com.cyna.modules.product.domain.model.CarouselSettingsTranslation;

import java.util.Map;

public record OfferCarouselSettingsResponse(
        Map<String, CarouselSettingsTranslation> translations
) {
    public static OfferCarouselSettingsResponse from(OfferCarouselSettingsReadModel model) {
        return new OfferCarouselSettingsResponse(model.translations());
    }
}
