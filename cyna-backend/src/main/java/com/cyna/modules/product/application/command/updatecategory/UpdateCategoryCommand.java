package com.cyna.modules.product.application.command.updatecategory;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record UpdateCategoryCommand(
        UUID id,
        String name,
        String fullName,
        String description
) implements Command<UUID> {}
