package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.modules.product.domain.model.PromotionTranslation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record UpdatePromotionRequest(
        @Min(1) @Max(100) int discountPercent,
        @NotNull @Valid Map<String, PromotionTranslation> translations,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        boolean enabled,
        boolean showInCarousel,
        @Min(1) Integer carouselOrder
) {}
