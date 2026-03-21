package com.cyna.modules.cart.interfaces.dto.request;

import com.cyna.modules.cart.domain.model.BillingCycle;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddCartLineRequest(
        @NotNull(message = "Product id is required")
        UUID productId,

        @NotNull(message = "Billing cycle is required")
        BillingCycle billingCycle,

        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 99, message = "Quantity must be at most 99")
        int quantity
) {
}
