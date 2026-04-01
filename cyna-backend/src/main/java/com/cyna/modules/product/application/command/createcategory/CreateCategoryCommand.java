package com.cyna.modules.product.application.command.createcategory;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record CreateCategoryCommand(
        String name,
        String description
) implements Command<UUID> {}
