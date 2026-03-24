package com.cyna.modules.cart.interfaces.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateCartLineQuantityRequest(
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 99, message = "Quantity must be at most 99")
        int quantity
) {
}
