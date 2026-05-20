package com.cyna.modules.product.application.command.update;

import com.cyna.shared.application.Command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateProductCommand(
        UUID id,
        String name,
        String nameEn,
        UUID categoryId,
        int priorityLevel,
        String serviceDescription,
        String serviceDescriptionEn,
        String technicalDescription,
        String technicalDescriptionEn,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        String currency,
        int freeTrialDays,
        List<String> highlightPoints,
        List<String> highlightPointsEn,
        boolean isPublished,
        boolean isAvailable
) implements Command<UUID> {}
