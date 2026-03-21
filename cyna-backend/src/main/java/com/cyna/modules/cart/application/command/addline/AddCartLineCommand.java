package com.cyna.modules.cart.application.command.addline;

import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.modules.cart.domain.model.BillingCycle;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record AddCartLineCommand(
        UUID userId,
        String guestToken,
        UUID productId,
        BillingCycle billingCycle,
        int quantity
) implements Command<CartReadModel> {
}
