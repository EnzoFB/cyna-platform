package com.cyna.modules.cart.application.command.mergeguest;

import com.cyna.modules.cart.application.model.CartReadModel;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record MergeGuestCartCommand(
        UUID userId,
        String guestToken
) implements Command<CartReadModel> {
}
