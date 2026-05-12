package com.cyna.modules.subscription.application.command.create;

import com.cyna.modules.subscription.domain.model.BillingCycle;
import com.cyna.shared.application.Command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateSubscriptionCommand(
        UUID userId,
        UUID orderId,
        UUID orderLineId,
        UUID productId,
        String productName,
        String productCategory,
        BillingCycle billingCycle,
        int quantity,
        BigDecimal unitPrice,
        String currency,
        Instant startAt,
        Instant endAt,
        Instant nextBillingAt,
        String stripeSubscriptionId,
        String stripeScheduleId
) implements Command<com.cyna.modules.subscription.application.query.getbyid.SubscriptionReadModel> {}
