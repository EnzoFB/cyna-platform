package com.cyna.modules.payment.application.command.savedpaymentmethod;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record DeletePaymentMethodCommand(UUID paymentMethodId, UUID userId) implements Command<Void> {}
