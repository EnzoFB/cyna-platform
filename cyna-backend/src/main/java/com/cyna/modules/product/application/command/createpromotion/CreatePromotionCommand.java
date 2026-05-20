package com.cyna.modules.product.application.command.createpromotion;

import com.cyna.shared.application.Command;

import java.time.Instant;
import java.util.UUID;

public record CreatePromotionCommand(
        UUID productId,
        int discountPercent,
        String marketingTextFr,
        String marketingTextEn,
        Instant startAt,
        Instant endAt,
        boolean enabled,
        boolean showInCarousel,
        Integer carouselOrder
) implements Command<UUID> {
}
