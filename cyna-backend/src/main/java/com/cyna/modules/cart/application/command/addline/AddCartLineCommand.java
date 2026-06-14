package com.cyna.modules.cart.application.command.addline;

import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record AddCartLineCommand(
        UUID userId,
        UUID productId,
        BillingCycle billingCycle,
        int quantity
) implements Command<CartReadModel> {
}
