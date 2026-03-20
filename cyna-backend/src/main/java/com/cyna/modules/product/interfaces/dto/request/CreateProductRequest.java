package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.modules.product.domain.model.ProductCategory;
import com.cyna.modules.product.domain.model.ProductPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @NotNull(message = "Category is required")
        ProductCategory category,

        @NotNull(message = "Priority is required")
        ProductPriority priority,

        @NotBlank(message = "Service description is required")
        String serviceDescription,

        @NotBlank(message = "Technical description is required")
        String technicalDescription,

        @NotNull(message = "Monthly price is required")
        BigDecimal monthlyPrice,

        @NotNull(message = "Annual price is required")
        BigDecimal annualPrice,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
        String currency
) {}
