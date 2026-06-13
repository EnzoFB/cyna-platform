package com.cyna.modules.cart.application.command.updatebillingcycle;

import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record UpdateCartLineBillingCycleCommand(
        UUID userId,
        UUID lineId,
        BillingCycle billingCycle
) implements Command<CartReadModel> {
}
