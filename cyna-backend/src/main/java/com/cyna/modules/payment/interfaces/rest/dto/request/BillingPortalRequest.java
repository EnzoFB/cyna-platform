package com.cyna.modules.payment.interfaces.rest.dto.request;

import com.cyna.shared.validation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BillingPortalRequest(
        @NotBlank(message = "returnUrl is required")
        @Pattern(
                regexp = "^https?://.+",
                message = "returnUrl must be an absolute http(s) URL"
        )
        @NoHtml(message = "Return URL must not contain HTML")
        String returnUrl
) {}
