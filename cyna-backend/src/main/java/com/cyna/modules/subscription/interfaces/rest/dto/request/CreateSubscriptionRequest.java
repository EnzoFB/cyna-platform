package com.cyna.modules.subscription.interfaces.rest.dto.request;

import com.cyna.modules.subscription.domain.model.BillingCycle;
import com.cyna.shared.interfaces.rest.validation.NoHtml;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateSubscriptionRequest(
        @NotNull(message = "Order id is required")
        UUID orderId,

        @NotNull(message = "Product id is required")
        UUID productId,

        @NotBlank(message = "Product name is required")
        @Size(max = 200, message = "Product name must not exceed 200 characters")
        @NoHtml(message = "Product name must not contain HTML")
        String productName,

        @NotBlank(message = "Product category is required")
        @Size(max = 200, message = "Product category must not exceed 200 characters")
        @NoHtml(message = "Product category must not contain HTML")
        String productCategory,

        @NotNull(message = "Billing cycle is required")
        BillingCycle billingCycle,

        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 99, message = "Quantity must be at most 99")
        int quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Unit price must be positive")
        BigDecimal unitPrice,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
        String currency,

        @NotNull(message = "Start date is required")
        Instant startAt,

        @NotNull(message = "End date is required")
        Instant endAt,

        @NotNull(message = "Next billing date is required")
        Instant nextBillingAt
) {
}
