package com.cyna.modules.cart.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MergeGuestCartRequest(
        @NotBlank(message = "Guest token is required")
        String guestToken
) {
}
