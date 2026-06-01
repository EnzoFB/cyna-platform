package com.cyna.modules.product.domain.model;

import com.cyna.shared.interfaces.rest.validation.NoHtml;

public record PromotionTranslation(
        @NoHtml(message = "Marketing text must not contain HTML")
        String marketingText
) {}
