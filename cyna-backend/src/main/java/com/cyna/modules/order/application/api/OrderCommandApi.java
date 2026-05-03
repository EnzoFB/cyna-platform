package com.cyna.modules.order.application.api;

import com.cyna.shared.domain.Result;

import java.util.UUID;

public interface OrderCommandApi {
    Result<Void> markOrderAsPaid(UUID orderId);
}
