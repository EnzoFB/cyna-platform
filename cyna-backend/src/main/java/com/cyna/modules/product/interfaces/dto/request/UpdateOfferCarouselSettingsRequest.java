package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.modules.product.domain.model.CarouselSettingsTranslation;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateOfferCarouselSettingsRequest(
        @NotNull Map<String, CarouselSettingsTranslation> translations
) {}
