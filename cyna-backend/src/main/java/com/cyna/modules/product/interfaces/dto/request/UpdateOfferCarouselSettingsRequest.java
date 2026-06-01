package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.shared.interfaces.rest.validation.NoHtml;
import jakarta.validation.constraints.NotNull;

public record UpdateOfferCarouselSettingsRequest(
        @NotNull @NoHtml(message = "Fixed text (FR) must not contain HTML")
        String fixedTextFr,
        @NotNull @NoHtml(message = "Fixed text (EN) must not contain HTML")
        String fixedTextEn
) {
}
