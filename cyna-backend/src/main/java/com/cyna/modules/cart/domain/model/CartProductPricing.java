package com.cyna.modules.cart.domain.model;

import com.cyna.shared.domain.Guard;

import java.math.BigDecimal;
import java.util.UUID;

public record CartProductPricing(
        UUID productId,
        BigDecimal monthlyPrice,
        BigDecimal annualPrice,
        String currency,
        boolean published
) {
    public CartProductPricing {
        Guard.againstNull(productId, "productId");
        Guard.againstNull(monthlyPrice, "monthlyPrice");
        Guard.againstNull(annualPrice, "annualPrice");
        Guard.againstNullOrBlank(currency, "currency");
    }
}
