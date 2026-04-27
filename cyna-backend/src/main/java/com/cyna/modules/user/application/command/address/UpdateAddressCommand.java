package com.cyna.modules.user.application.command.address;

import com.cyna.shared.application.Command;

import java.util.UUID;

public record UpdateAddressCommand(
        UUID addressId,
        UUID userId,
        String firstName,
        String lastName,
        String label,
        String address,
        String address2,
        String zipCode,
        String city,
        String region,
        String countryCode,
        String phone,
        String company,
        String vatNumber
) implements Command<Void> {}
