package com.cyna.modules.product.interfaces.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreatePromotionRequest(
        @NotNull UUID productId,
        @Min(1) @Max(100) int discountPercent,
        @NotBlank String marketingTextFr,
        @NotBlank String marketingTextEn,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        boolean enabled
) {
}

