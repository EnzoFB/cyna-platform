package com.cyna.modules.user.application.command.create;

import com.cyna.modules.user.application.port.PasswordHasher;
import com.cyna.modules.user.domain.model.*;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.application.DomainEventPublisher;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateAdminUserCommandHandler implements CommandHandler<CreateAdminUserCommand, UUID> {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final DomainEventPublisher eventPublisher;
    private final TransactionRunner transactionRunner;

    public CreateAdminUserCommandHandler(UserRepository userRepository,
                                         PasswordHasher passwordHasher,
                                         DomainEventPublisher eventPublisher,
                                         TransactionRunner transactionRunner) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.eventPublisher = eventPublisher;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public Result<UUID> handle(CreateAdminUserCommand command) {
        var email = Email.of(command.email());

        if (userRepository.existsByEmail(email)) {
            return Result.failure("Email already exists");
        }

        Role role;
        try {
            role = Role.valueOf(command.role());
        } catch (IllegalArgumentException e) {
            return Result.failure("Invalid role");
        }

        return transactionRunner.runReturning(() -> {
            HashedPassword hashedPassword = passwordHasher.hash(command.password());

            User user = User.createByAdmin(email, hashedPassword,
                    command.firstName(), command.lastName(), role);
            userRepository.save(user);

            eventPublisher.publishAll(user.getDomainEvents());
            user.clearDomainEvents();

            return Result.success(user.getId());
        });
    }
}
