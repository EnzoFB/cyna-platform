package com.cyna.modules.cart.application.command.checkout;

import com.cyna.modules.cart.application.model.CheckoutCartReadModel;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record CheckoutCartCommand(
        UUID userId
) implements Command<CheckoutCartReadModel> {
}
