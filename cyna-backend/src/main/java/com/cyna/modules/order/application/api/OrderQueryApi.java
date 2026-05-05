package com.cyna.modules.order.application.api;

import java.util.Optional;
import java.util.UUID;

public interface OrderQueryApi {
    Optional<OrderPaymentView> findOrderForPayment(UUID orderId, UUID userId);
}
