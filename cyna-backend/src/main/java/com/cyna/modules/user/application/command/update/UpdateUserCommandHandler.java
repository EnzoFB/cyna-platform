package com.cyna.modules.user.application.command.update;

import com.cyna.modules.user.domain.model.Role;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.model.UserStatus;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class UpdateUserCommandHandler implements CommandHandler<UpdateUserCommand, Void> {

    private final UserRepository userRepository;
    private final TransactionRunner transactionRunner;

    public UpdateUserCommandHandler(UserRepository userRepository,
                                     TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(UpdateUserCommand command) {
        var userId = command.userId();

        var optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return Result.failure("User not found");
        }

        Role role;
        try {
            role = Role.valueOf(command.role());
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid role: " + command.role());
        }

        UserStatus status;
        try {
            status = UserStatus.valueOf(command.status());
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid status: " + command.status());
        }

        User user = optionalUser.get();
        User updated = user.updateInfo(command.firstName(), command.lastName(), role, status);

        transactionRunner.run(() -> userRepository.save(updated));

        return Result.success();
    }
}
