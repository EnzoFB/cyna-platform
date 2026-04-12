package com.cyna.modules.cart.application.command.updatequantity;

import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record UpdateCartLineQuantityCommand(
        UUID userId,
        UUID lineId,
        int quantity
) implements Command<CartReadModel> {
}
