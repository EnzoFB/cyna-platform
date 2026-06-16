package com.cyna.modules.order.interfaces.rest.dto.request;

import com.cyna.shared.validation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 255, message = "Cancellation reason must not exceed 255 characters")
        @NoHtml(message = "Cancellation reason must not contain HTML")
        String reason
) {
}
