package com.cyna.modules.product.application.command.createcategory;

import com.cyna.modules.product.domain.model.CategoryTranslation;
import com.cyna.shared.application.Command;

import java.util.Map;
import java.util.UUID;

public record CreateCategoryCommand(
        String name,
        Map<String, CategoryTranslation> translations
) implements Command<UUID> {}
