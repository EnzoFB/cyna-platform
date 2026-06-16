package com.cyna.modules.payment.application.command.initiate;

import com.cyna.modules.payment.application.model.PaymentInitiatedReadModel;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record InitiatePaymentCommand(
        UUID orderId,
        UUID userId
) implements Command<PaymentInitiatedReadModel> {}
