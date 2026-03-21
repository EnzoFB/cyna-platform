package com.cyna.modules.cart.domain.model;

import com.cyna.shared.domain.Guard;

import java.math.BigDecimal;

public record CartTotals(
        BigDecimal subtotalHt,
        BigDecimal vatAmount,
        BigDecimal totalTtc,
        String currency
) {
    public CartTotals {
        Guard.againstNull(subtotalHt, "subtotalHt");
        Guard.againstNull(vatAmount, "vatAmount");
        Guard.againstNull(totalTtc, "totalTtc");
        Guard.againstNullOrBlank(currency, "currency");
    }
}
