package com.cyna.modules.product.application.command.reorderimages;

import com.cyna.shared.application.Command;

import java.util.List;
import java.util.UUID;

public record ReorderProductImagesCommand(UUID productId, List<UUID> orderedImageIds) implements Command<Void> {}
