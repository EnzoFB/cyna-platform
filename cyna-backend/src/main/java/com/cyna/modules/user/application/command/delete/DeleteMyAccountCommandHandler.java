package com.cyna.modules.user.application.command.delete;

import com.cyna.shared.application.CommandHandler;
import com.cyna.shared.domain.Result;
import org.springframework.stereotype.Component;

/**
 * Self-service erasure handler. Same {@link AccountErasure} policy as the
 * admin path — the outcome (hard-delete vs anonymize) depends only on the
 * account's legal footprint.
 */
@Component
public class DeleteMyAccountCommandHandler
        implements CommandHandler<DeleteMyAccountCommand, Void> {

    private final AccountErasure accountErasure;

    public DeleteMyAccountCommandHandler(AccountErasure accountErasure) {
        this.accountErasure = accountErasure;
    }

    @Override
    public Result<Void> handle(DeleteMyAccountCommand command) {
        return accountErasure.erase(command.userId());
    }
}
