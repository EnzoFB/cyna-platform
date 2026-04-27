package com.cyna.modules.user.application.command.address;

import com.cyna.modules.user.domain.model.Address;
import com.cyna.modules.user.domain.repository.AddressRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateAddressCommandHandler implements CommandHandler<CreateAddressCommand, UUID> {

    private final AddressRepository addressRepository;
    private final TransactionRunner transactionRunner;

    public CreateAddressCommandHandler(AddressRepository addressRepository, TransactionRunner transactionRunner) {
        this.addressRepository = addressRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<UUID> handle(CreateAddressCommand command) {
        var existingAddresses = addressRepository.findAllByUserId(command.userId());
        boolean isFirst = existingAddresses.isEmpty();

        var address = Address.create(
                command.userId(), command.label(), command.address(), command.address2(),
                command.zipCode(), command.city(), command.region(), command.countryCode(),
                command.phone(), isFirst
        );

        transactionRunner.run(() -> addressRepository.save(address));

        return Result.success(address.getId());
    }
}
