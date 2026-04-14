package com.cyna.modules.user.application.command.delete;

import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class DeleteUserCommandHandler implements CommandHandler<DeleteUserCommand, Void> {

    private final UserRepository userRepository;
    private final TransactionRunner transactionRunner;

    public DeleteUserCommandHandler(UserRepository userRepository,
                                     TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(DeleteUserCommand command) {
        var userId = command.userId();

        var optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return Result.failure("User not found");
        }

        transactionRunner.run(() -> userRepository.deleteById(userId));

        return Result.success();
    }
}
