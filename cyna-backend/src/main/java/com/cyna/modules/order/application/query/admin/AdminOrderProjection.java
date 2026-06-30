package com.cyna.modules.order.application.query.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface AdminOrderProjection {
    UUID getId();
    UUID getUserId();
    String getStatus();
    BigDecimal getSubtotalAmount();
    String getCurrency();
    Instant getCreatedAt();
    Instant getUpdatedAt();
    Long getLineCount();
}
