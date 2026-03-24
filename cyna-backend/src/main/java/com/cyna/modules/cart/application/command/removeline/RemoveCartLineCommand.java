package com.cyna.modules.cart.application.command.removeline;

import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record RemoveCartLineCommand(
        UUID userId,
        String guestToken,
        UUID lineId
) implements Command<CartReadModel> {
}
