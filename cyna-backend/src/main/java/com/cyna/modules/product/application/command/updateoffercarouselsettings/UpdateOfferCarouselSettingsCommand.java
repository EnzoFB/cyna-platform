package com.cyna.modules.product.application.command.updateoffercarouselsettings;

import com.cyna.modules.product.application.translation.CarouselSettingsTranslationDto;
import com.cyna.shared.application.Command;

import java.util.Map;

public record UpdateOfferCarouselSettingsCommand(
        Map<String, CarouselSettingsTranslationDto> translations,
        int maxSlides
) implements Command<Void> {}
