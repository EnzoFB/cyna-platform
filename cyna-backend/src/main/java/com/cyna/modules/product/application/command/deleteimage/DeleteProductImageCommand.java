package com.cyna.modules.product.application.command.deleteimage;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record DeleteProductImageCommand(UUID productId, UUID imageId) implements Command<Void> {}
