package com.cyna.modules.product.application.command.addimage;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record AddProductImageCommand(UUID productId, byte[] imageData, String mimeType) implements Command<UUID> {}
