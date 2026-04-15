package com.cyna.modules.order.application.command.cancel;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record CancelOrderCommand(
        UUID orderId,
        UUID userId,
        String reason
) implements Command<UUID> {
}
