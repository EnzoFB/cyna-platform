package com.cyna.modules.user.application.command.password;

import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class ChangePasswordCommandHandler implements CommandHandler<ChangePasswordCommand, Void> {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TransactionRunner transactionRunner;

    public ChangePasswordCommandHandler(UserRepository userRepository,
                                        PasswordHasher passwordHasher,
                                        TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(ChangePasswordCommand command) {
        var optUser = userRepository.findById(command.userId());
        if (optUser.isEmpty()) {
            return Result.failure("User not found");
        }

        var user = optUser.get();

        if (!passwordHasher.matches(command.currentPassword(), user.getHashedPassword())) {
            return Result.failure("Current password is incorrect");
        }

        var newHashed = passwordHasher.hash(command.newPassword());
        var updated = user.changePassword(newHashed);

        transactionRunner.run(() -> userRepository.save(updated));

        return Result.success();
    }
}
