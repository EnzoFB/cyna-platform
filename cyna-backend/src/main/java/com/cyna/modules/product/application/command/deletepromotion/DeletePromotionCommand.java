package com.cyna.modules.product.application.command.deletepromotion;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record DeletePromotionCommand(UUID id) implements Command<Void> {
}

