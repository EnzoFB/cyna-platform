package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.modules.product.domain.model.ProductTranslation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CreateProductRequest(
        @NotNull(message = "Translations are required")
        @Valid
        Map<String, ProductTranslation> translations,

        @NotNull(message = "Category is required")
        UUID categoryId,

        int priorityLevel,

        @NotNull(message = "Monthly price is required")
        BigDecimal monthlyPrice,

        @NotNull(message = "Annual price is required")
        BigDecimal annualPrice,

        @NotNull(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
        String currency,

        int freeTrialDays
) {}
