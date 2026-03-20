package com.cyna.modules.product.interfaces.dto.response;

import java.math.BigDecimal;

public record PriceResponse(
        BigDecimal amount,
        String currency
) {}
