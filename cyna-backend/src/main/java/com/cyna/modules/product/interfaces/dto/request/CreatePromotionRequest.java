package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.modules.product.application.translation.PromotionTranslationDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CreatePromotionRequest(
        @NotNull UUID productId,
        @Min(1) @Max(100) int discountPercent,
        @NotNull @Valid Map<String, PromotionTranslationDto> translations,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        boolean enabled
) {}
