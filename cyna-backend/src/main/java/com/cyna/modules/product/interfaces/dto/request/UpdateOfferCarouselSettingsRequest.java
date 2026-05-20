package com.cyna.modules.product.interfaces.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateOfferCarouselSettingsRequest(
        @NotNull String fixedTextFr,
        @NotNull String fixedTextEn
) {
}
