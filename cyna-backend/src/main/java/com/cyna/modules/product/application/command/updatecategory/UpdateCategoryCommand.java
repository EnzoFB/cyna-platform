package com.cyna.modules.product.application.command.updatecategory;

import com.cyna.modules.product.domain.model.CategoryTranslation;
import com.cyna.shared.application.Command;

import java.util.Map;
import java.util.UUID;

public record UpdateCategoryCommand(
        UUID id,
        String name,
        Map<String, CategoryTranslation> translations,
        boolean active
) implements Command<UUID> {}
