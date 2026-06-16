package com.cyna.modules.product.application.command.bulkdelete;

import com.cyna.shared.application.Command;

import java.util.List;
import java.util.UUID;

public record BulkDeleteProductsCommand(
        List<UUID> ids
) implements Command<Void> {}
