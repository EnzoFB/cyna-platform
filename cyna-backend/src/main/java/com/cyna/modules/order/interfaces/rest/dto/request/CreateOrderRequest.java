package com.cyna.modules.order.interfaces.rest.dto.request;

import com.cyna.shared.domain.BillingCycle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotEmpty(message = "Order lines are required")
        @Valid
        List<CreateOrderLineRequest> lines,

        @Valid
        BillingAddressRequest billingAddress
) {
    public record CreateOrderLineRequest(
            @NotNull(message = "Product id is required")
            UUID productId,

            @NotNull(message = "Billing cycle is required")
            BillingCycle billingCycle,

            @Min(value = 1, message = "Quantity must be at least 1")
            @Max(value = 99, message = "Quantity must be at most 99")
            int quantity
    ) {
    }

    public record BillingAddressRequest(
            String line1,
            String city,
            String zipCode,
            String countryCode
    ) {
    }
}
