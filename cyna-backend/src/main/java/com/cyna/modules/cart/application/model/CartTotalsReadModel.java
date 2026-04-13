package com.cyna.modules.cart.application.model;

import java.math.BigDecimal;

public record CartTotalsReadModel(
        BigDecimal subtotalHt,
        BigDecimal vatAmount,
        BigDecimal totalTtc,
        String currency
) {
}
