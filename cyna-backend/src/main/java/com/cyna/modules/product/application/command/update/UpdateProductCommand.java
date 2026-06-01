package com.cyna.modules.product.application.command.update;

import com.cyna.modules.product.domain.model.ProductTranslation;
import com.cyna.shared.application.Command;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record UpdateProductCommand(
        UUID id,
        Map<String, ProductTranslation> translations,
        UUID categoryId,
        int priorityLevel,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        String currency,
        int freeTrialDays,
        boolean isPublished,
        boolean isAvailable
) implements Command<UUID> {}
