package com.cyna.modules.product.application.command.updatepromotion;

import com.cyna.modules.product.domain.model.PromotionTranslation;
import com.cyna.shared.application.Command;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UpdatePromotionCommand(
        UUID id,
        int discountPercent,
        Map<String, PromotionTranslation> translations,
        Instant startAt,
        Instant endAt,
        boolean enabled,
        boolean showInCarousel,
        Integer carouselOrder
) implements Command<UUID> {}
