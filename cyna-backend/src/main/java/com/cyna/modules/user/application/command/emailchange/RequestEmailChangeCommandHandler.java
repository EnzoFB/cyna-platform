package com.cyna.modules.user.application.command.emailchange;

import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.EmailChangeToken;
import com.cyna.modules.user.domain.repository.EmailChangeTokenRepository;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.modules.user.domain.event.EmailChangeRequested;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class RequestEmailChangeCommandHandler implements CommandHandler<RequestEmailChangeCommand, Void> {

    private final UserRepository userRepository;
    private final EmailChangeTokenRepository tokenRepository;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public RequestEmailChangeCommandHandler(UserRepository userRepository,
                                            EmailChangeTokenRepository tokenRepository,
                                            DomainEventPublisher eventPublisher,
                                            TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<Void> handle(RequestEmailChangeCommand command) {
        var optUser = userRepository.findById(command.userId());
        if (optUser.isEmpty()) {
            return Result.failure("User not found");
        }

        var user = optUser.get();
        var newEmail = Email.of(command.newEmail());

        if (userRepository.existsByEmail(newEmail)) {
            return Result.failure("Email already in use");
        }

        String rawToken = UUID.randomUUID().toString();
        var token = EmailChangeToken.create(
                command.userId(),
                command.newEmail(),
                rawToken,
                Instant.now().plus(Duration.ofHours(24))
        );

        transactionRunner.run(() -> {
            tokenRepository.deleteByUserId(command.userId());
            tokenRepository.save(token);

            eventPublisher.publish(new EmailChangeRequested(
                    command.userId(),
                    command.newEmail(),
                    user.getFirstName(),
                    rawToken,
                    command.lang(),
                    Instant.now()
            ));
        });

        return Result.success();
    }
}
