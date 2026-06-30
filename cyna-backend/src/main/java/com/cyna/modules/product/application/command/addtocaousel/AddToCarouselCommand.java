package com.cyna.modules.product.application.command.addtocaousel;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record AddToCarouselCommand(UUID promotionId) implements Command<Void> {}
