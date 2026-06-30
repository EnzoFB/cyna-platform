package com.cyna.modules.payment.application.query.getbyid;

import com.cyna.shared.application.Query;

import java.util.UUID;

public record GetPaymentByOrderIdQuery(UUID orderId, UUID userId) implements Query<PaymentReadModel> {}
