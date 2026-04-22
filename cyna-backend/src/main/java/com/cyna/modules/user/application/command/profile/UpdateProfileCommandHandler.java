package com.cyna.modules.user.application.command.profile;

import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UpdateProfileCommandHandler implements CommandHandler<UpdateProfileCommand, Void> {

    private final UserRepository userRepository;
    private final TransactionRunner transactionRunner;

    public UpdateProfileCommandHandler(UserRepository userRepository, TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(UpdateProfileCommand command) {
        UUID userId = command.userId();

        var optUser = userRepository.findById(userId);
        if (optUser.isEmpty()) {
            return Result.failure("User not found");
        }

        var updated = optUser.get().updateProfile(
                command.firstName(),
                command.lastName(),
                command.company()
        );

        transactionRunner.run(() -> userRepository.save(updated));

        return Result.success();
    }
}
