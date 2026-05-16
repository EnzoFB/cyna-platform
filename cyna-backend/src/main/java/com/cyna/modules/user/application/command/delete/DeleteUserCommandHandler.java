package com.cyna.modules.user.application.command.delete;

import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

/**
 * Admin-initiated erasure ({@code DELETE /api/v1/admin/users/{id}}).
 * Delegates to the shared {@link AccountErasure} policy — admin and
 * self-service erasure must behave identically (hard-delete vs anonymize
 * decided by the account's legal footprint, never by who triggered it).
 */
@Component
public class DeleteUserCommandHandler implements CommandHandler<DeleteUserCommand, Void> {

    private final AccountErasure accountErasure;

    public DeleteUserCommandHandler(AccountErasure accountErasure) {
        this.accountErasure = accountErasure;
    }

    @Override
    public Result<Void> handle(DeleteUserCommand command) {
        return accountErasure.erase(command.userId());
    }
}
