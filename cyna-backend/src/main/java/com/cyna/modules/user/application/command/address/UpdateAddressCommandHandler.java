package com.cyna.modules.user.application.command.address;

import com.cyna.modules.user.domain.repository.AddressRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class UpdateAddressCommandHandler implements CommandHandler<UpdateAddressCommand, Void> {

    private final AddressRepository addressRepository;
    private final TransactionRunner transactionRunner;

    public UpdateAddressCommandHandler(AddressRepository addressRepository, TransactionRunner transactionRunner) {
        this.addressRepository = addressRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(UpdateAddressCommand command) {
        var opt = addressRepository.findById(command.addressId());
        if (opt.isEmpty()) return Result.failure("Address not found");

        var address = opt.get();
        if (!address.getUserId().equals(command.userId())) return Result.failure("Forbidden");

        var updated = address.update(
                command.firstName(), command.lastName(), command.label(),
                command.address(), command.address2(), command.zipCode(),
                command.city(), command.region(), command.countryCode(),
                command.phone(), command.company(), command.vatNumber()
        );

        transactionRunner.run(() -> addressRepository.save(updated));
        return Result.success();
    }
}
