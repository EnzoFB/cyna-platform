package com.cyna.modules.product.application.command.createcategory;

import com.cyna.modules.product.application.translation.CategoryTranslationDto;
import com.cyna.shared.application.Command;

import java.util.Map;
import java.util.UUID;

public record CreateCategoryCommand(
        String name,
        Map<String, CategoryTranslationDto> translations
) implements Command<UUID> {}
