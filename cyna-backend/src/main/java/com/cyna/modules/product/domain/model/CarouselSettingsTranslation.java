package com.cyna.modules.product.domain.model;

import com.cyna.shared.interfaces.rest.validation.NoHtml;

public record CarouselSettingsTranslation(
        @NoHtml(message = "Fixed text must not contain HTML")
        String fixedText
) {}
