package com.cyna.modules.user.application.command.address;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record SetDefaultAddressCommand(UUID addressId, UUID userId) implements Command<Void> {}
