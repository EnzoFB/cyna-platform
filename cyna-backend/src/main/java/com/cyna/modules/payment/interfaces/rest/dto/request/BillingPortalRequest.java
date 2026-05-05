package com.cyna.modules.payment.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BillingPortalRequest(
        @NotBlank(message = "returnUrl is required")
        @Pattern(
                regexp = "^https?://.+",
                message = "returnUrl must be an absolute http(s) URL"
        )
        String returnUrl
) {}
