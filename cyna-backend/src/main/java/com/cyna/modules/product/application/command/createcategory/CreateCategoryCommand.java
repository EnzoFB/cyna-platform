package com.cyna.modules.product.application.command.createcategory;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record CreateCategoryCommand(
        String name,
        String fullName,
        String fullNameEn,
        String description,
        String descriptionEn
) implements Command<UUID> {}
