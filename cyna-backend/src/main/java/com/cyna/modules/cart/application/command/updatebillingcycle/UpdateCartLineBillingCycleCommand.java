package com.cyna.modules.cart.application.command.updatebillingcycle;

import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.modules.cart.domain.model.BillingCycle;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record UpdateCartLineBillingCycleCommand(
        UUID userId,
        String guestToken,
        UUID lineId,
        BillingCycle billingCycle
) implements Command<CartReadModel> {
}
