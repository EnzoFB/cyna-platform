package com.cyna.modules.product.interfaces.dto.response;

import com.cyna.modules.product.application.query.getoffercarouselsettings.OfferCarouselSettingsReadModel;

public record OfferCarouselSettingsResponse(
        String fixedTextFr,
        String fixedTextEn
) {
    public static OfferCarouselSettingsResponse from(OfferCarouselSettingsReadModel model) {
        return new OfferCarouselSettingsResponse(
                model.fixedTextFr(),
                model.fixedTextEn()
        );
    }
}
