package com.cyna.modules.product.application.command.deletecategory;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record DeleteCategoryCommand(UUID id) implements Command<Void> {}
