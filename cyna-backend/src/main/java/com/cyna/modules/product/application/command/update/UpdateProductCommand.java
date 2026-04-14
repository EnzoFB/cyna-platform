package com.cyna.modules.product.application.command.update;

import com.cyna.modules.product.domain.model.ProductCategory;
import com.cyna.modules.product.domain.model.ProductPriority;
import com.cyna.shared.application.Command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductCommand(
        UUID id,
        String name,
        UUID categoryId,
        int priorityLevel,
        String serviceDescription,
        String technicalDescription,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        String currency
) implements Command<UUID> {}
