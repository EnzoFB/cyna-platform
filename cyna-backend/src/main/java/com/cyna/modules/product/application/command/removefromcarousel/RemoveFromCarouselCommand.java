package com.cyna.modules.product.application.command.removefromcarousel;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record RemoveFromCarouselCommand(UUID promotionId) implements Command<Void> {}
