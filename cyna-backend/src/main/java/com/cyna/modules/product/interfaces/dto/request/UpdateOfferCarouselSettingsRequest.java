package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.modules.product.domain.model.CarouselSettingsTranslation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateOfferCarouselSettingsRequest(
        @NotNull @Valid Map<String, CarouselSettingsTranslation> translations,
        @NotNull @Min(1) @Max(20) Integer maxSlides
) {}
