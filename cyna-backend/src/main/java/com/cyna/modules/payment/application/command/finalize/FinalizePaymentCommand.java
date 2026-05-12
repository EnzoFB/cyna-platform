package com.cyna.modules.payment.application.command.finalize;

import com.cyna.modules.payment.application.model.PaymentFinalizedReadModel;
import com.cyna.shared.application.Command;

import java.util.UUID;

public record FinalizePaymentCommand(UUID orderId, UUID userId, String paymentMethodId) implements Command<PaymentFinalizedReadModel> {}
