package com.cyna.modules.order.application.command.create;

import com.cyna.shared.domain.BillingCycle;
import com.cyna.shared.application.Command;

import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
        UUID userId,
        List<CreateOrderLine> lines
) implements Command<UUID> {
    public record CreateOrderLine(
            UUID productId,
            BillingCycle billingCycle,
            int quantity
    ) {
    }
}
