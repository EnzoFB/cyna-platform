package com.cyna.modules.user.application.command.address;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record DeleteAddressCommand(UUID addressId, UUID userId) implements Command<Void> {}
