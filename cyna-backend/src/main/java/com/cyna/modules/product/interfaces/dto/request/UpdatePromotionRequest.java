package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.shared.interfaces.rest.validation.NoHtml;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpdatePromotionRequest(
        @Min(1) @Max(100) int discountPercent,
        @NotBlank @NoHtml(message = "Marketing text (FR) must not contain HTML")
        String marketingTextFr,
        @NotBlank @NoHtml(message = "Marketing text (EN) must not contain HTML")
        String marketingTextEn,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        boolean enabled,
        boolean showInCarousel,
        @Min(1) Integer carouselOrder
) {
}
