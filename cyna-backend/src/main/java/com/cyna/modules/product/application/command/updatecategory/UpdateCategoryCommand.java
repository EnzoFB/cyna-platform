package com.cyna.modules.product.application.command.updatecategory;

import com.cyna.modules.product.application.translation.CategoryTranslationDto;
import com.cyna.shared.application.Command;

import java.util.Map;
import java.util.UUID;

public record UpdateCategoryCommand(
        UUID id,
        String name,
        Map<String, CategoryTranslationDto> translations,
        boolean active
) implements Command<UUID> {}
