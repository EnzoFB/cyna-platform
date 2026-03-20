package com.cyna.modules.product.application.command.create;

import com.cyna.modules.product.domain.model.ProductCategory;
import com.cyna.modules.product.domain.model.ProductPriority;
import com.cyna.shared.application.Command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductCommand(
        String name,
        ProductCategory category,
        ProductPriority priority,
        String serviceDescription,
        String technicalDescription,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        String currency
) implements Command<UUID> {}
