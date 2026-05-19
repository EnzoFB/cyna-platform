package com.cyna.modules.product.application.command.updatepromotion;

import com.cyna.shared.application.Command;

import java.time.Instant;
import java.util.UUID;

public record UpdatePromotionCommand(
        UUID id,
        int discountPercent,
        String marketingTextFr,
        String marketingTextEn,
        Instant startAt,
        Instant endAt,
        boolean enabled
) implements Command<UUID> {
}

