package com.cyna.modules.product.application.command.create;

import com.cyna.modules.product.domain.model.ProductTranslation;
import com.cyna.shared.application.Command;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CreateProductCommand(
        Map<String, ProductTranslation> translations,
        UUID categoryId,
        int priorityLevel,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        String currency,
        int freeTrialDays
) implements Command<UUID> {}
