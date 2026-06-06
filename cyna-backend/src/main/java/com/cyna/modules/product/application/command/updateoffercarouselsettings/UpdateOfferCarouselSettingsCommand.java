package com.cyna.modules.product.application.command.updateoffercarouselsettings;

import com.cyna.modules.product.domain.model.CarouselSettingsTranslation;
import com.cyna.shared.application.Command;

import java.util.Map;

public record UpdateOfferCarouselSettingsCommand(
        Map<String, CarouselSettingsTranslation> translations,
        int maxSlides
) implements Command<Void> {}
