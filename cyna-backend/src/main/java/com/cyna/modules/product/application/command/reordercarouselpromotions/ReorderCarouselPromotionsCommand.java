package com.cyna.modules.product.application.command.reordercarouselpromotions;

import com.cyna.shared.application.Command;

import java.util.List;
import java.util.UUID;

/**
 * Reorders carousel promotions atomically.
 * orderedIds must contain exactly the IDs of all promotions currently in the carousel,
 * in the desired display order (index 0 = position 1).
 */
public record ReorderCarouselPromotionsCommand(
        List<UUID> orderedIds
) implements Command<Void> {}
