package com.cyna.modules.product.application.command.bulkdeletecategory;

import com.cyna.shared.application.Command;

import java.util.List;
import java.util.UUID;

public record BulkDeleteCategoriesCommand(
        List<UUID> ids
) implements Command<Void> {}
