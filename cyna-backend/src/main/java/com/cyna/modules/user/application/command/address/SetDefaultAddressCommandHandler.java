package com.cyna.modules.user.application.command.address;

import com.cyna.modules.user.domain.repository.AddressRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class SetDefaultAddressCommandHandler implements CommandHandler<SetDefaultAddressCommand, Void> {

    private final AddressRepository addressRepository;
    private final TransactionRunner transactionRunner;

    public SetDefaultAddressCommandHandler(AddressRepository addressRepository, TransactionRunner transactionRunner) {
        this.addressRepository = addressRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(SetDefaultAddressCommand command) {
        var opt = addressRepository.findById(command.addressId());
        if (opt.isEmpty()) return Result.failure("Address not found");

        var address = opt.get();
        if (!address.getUserId().equals(command.userId())) return Result.failure("Forbidden");

        transactionRunner.run(() -> {
            addressRepository.clearDefaultForUser(command.userId());
            addressRepository.save(address.withDefault(true));
        });

        return Result.success();
    }
}
