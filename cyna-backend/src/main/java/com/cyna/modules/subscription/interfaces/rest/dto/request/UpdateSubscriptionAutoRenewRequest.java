package com.cyna.modules.subscription.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateSubscriptionAutoRenewRequest(
        @NotNull(message = "autoRenew is required")
        Boolean autoRenew
) {
}
