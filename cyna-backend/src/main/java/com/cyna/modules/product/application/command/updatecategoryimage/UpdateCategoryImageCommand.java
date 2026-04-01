package com.cyna.modules.product.application.command.updatecategoryimage;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record UpdateCategoryImageCommand(UUID id, byte[] image) implements Command<Void> {}
