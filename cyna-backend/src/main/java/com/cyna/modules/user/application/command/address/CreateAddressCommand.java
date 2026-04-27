package com.cyna.modules.user.application.command.address;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record CreateAddressCommand(
        UUID userId,
        String label,
        String address,
        String address2,
        String zipCode,
        String city,
        String region,
        String countryCode,
        String phone
) implements Command<UUID> {}
