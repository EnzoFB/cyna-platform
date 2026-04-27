package com.cyna.modules.user.application.command.emailchange;

import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.repository.EmailChangeTokenRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

@Component
public class ConfirmEmailChangeCommandHandler implements CommandHandler<ConfirmEmailChangeCommand, Void> {

    private final EmailChangeTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final TransactionRunner transactionRunner;

    public ConfirmEmailChangeCommandHandler(EmailChangeTokenRepository tokenRepository,
                                            UserRepository userRepository,
                                            TransactionRunner transactionRunner) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(ConfirmEmailChangeCommand command) {
        var optToken = tokenRepository.findByToken(command.token());
        if (optToken.isEmpty()) {
            return Result.failure("Invalid or unknown token");
        }

        var changeToken = optToken.get();

        if (changeToken.isExpired()) {
            tokenRepository.deleteById(changeToken.id());
            return Result.failure("Token has expired");
        }

        var optUser = userRepository.findById(changeToken.userId());
        if (optUser.isEmpty()) {
            return Result.failure("User not found");
        }

        var updatedUser = optUser.get().withEmail(Email.of(changeToken.newEmail()));

        transactionRunner.run(() -> {
            userRepository.save(updatedUser);
            tokenRepository.deleteById(changeToken.id());
        });

        return Result.success();
    }
}
