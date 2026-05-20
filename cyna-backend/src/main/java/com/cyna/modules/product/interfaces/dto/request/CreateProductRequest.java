package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.shared.interfaces.rest.validation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        @NoHtml(message = "Name must not contain HTML")
        String name,

        @Size(max = 200, message = "Name (EN) must not exceed 200 characters")
        @NoHtml(message = "Name (EN) must not contain HTML")
        String nameEn,

        @NotNull(message = "Category is required")
        UUID categoryId,

        int priorityLevel,

        @NotBlank(message = "Service description is required")
        @NoHtml(message = "Service description must not contain HTML")
        String serviceDescription,

        @NoHtml(message = "Service description (EN) must not contain HTML")
        String serviceDescriptionEn,

        @NotBlank(message = "Technical description is required")
        @NoHtml(message = "Technical description must not contain HTML")
        String technicalDescription,

        @NoHtml(message = "Technical description (EN) must not contain HTML")
        String technicalDescriptionEn,

        @NotNull(message = "Monthly price is required")
        BigDecimal monthlyPrice,

        @NotNull(message = "Annual price is required")
        BigDecimal annualPrice,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
        String currency,

        int freeTrialDays,

        List<String> highlightPoints,

        List<String> highlightPointsEn
) {}
