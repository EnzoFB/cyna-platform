package com.cyna.modules.cart.interfaces.dto.request;

import com.cyna.shared.domain.BillingCycle;
import jakarta.validation.constraints.NotNull;

public record UpdateCartLineBillingCycleRequest(
        @NotNull(message = "Billing cycle is required")
        BillingCycle billingCycle
) {
}
