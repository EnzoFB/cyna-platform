package com.cyna.modules.product.application.command.delete;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record DeleteProductCommand(
        UUID id
) implements Command<Void> {}
