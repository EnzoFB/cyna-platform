package com.cyna.modules.product.application.command.updateoffercarouselsettings;

import com.cyna.shared.application.Command;

public record UpdateOfferCarouselSettingsCommand(
        String fixedTextFr,
        String fixedTextEn
) implements Command<Void> {
}
