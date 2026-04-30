package com.cyna.modules.user.application.command.address;

import com.cyna.modules.user.domain.repository.AddressRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class DeleteAddressCommandHandler implements CommandHandler<DeleteAddressCommand, Void> {

    private final AddressRepository addressRepository;
    private final TransactionRunner transactionRunner;

    public DeleteAddressCommandHandler(AddressRepository addressRepository, TransactionRunner transactionRunner) {
        this.addressRepository = addressRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(DeleteAddressCommand command) {
        var opt = addressRepository.findById(command.addressId());
        if (opt.isEmpty()) return Result.failure("Address not found");

        var address = opt.get();
        if (!address.getUserId().equals(command.userId())) return Result.failure("Forbidden");

        boolean wasDefault = address.isDefault();

        transactionRunner.run(() -> {
            addressRepository.deleteById(command.addressId());

            if (wasDefault) {
                var remaining = addressRepository.findAllByUserId(command.userId());
                if (!remaining.isEmpty()) {
                    var newDefault = remaining.stream()
                            .max(java.util.Comparator.comparing(a -> a.getUpdatedAt()))
                            .get();
                    addressRepository.save(newDefault.withDefault(true));
                }
            }
        });

        return Result.success();
    }
}
