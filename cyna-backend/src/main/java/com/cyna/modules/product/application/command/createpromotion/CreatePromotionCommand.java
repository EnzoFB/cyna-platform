package com.cyna.modules.product.application.command.createpromotion;

import com.cyna.modules.product.domain.model.PromotionTranslation;
import com.cyna.shared.application.Command;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CreatePromotionCommand(
        UUID productId,
        int discountPercent,
        Map<String, PromotionTranslation> translations,
        Instant startAt,
        Instant endAt,
        boolean enabled
) implements Command<UUID> {}
