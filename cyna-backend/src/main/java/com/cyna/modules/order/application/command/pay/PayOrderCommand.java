package com.cyna.modules.order.application.command.pay;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record PayOrderCommand(UUID orderId) implements Command<Void> {}
