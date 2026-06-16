package com.cyna.modules.account.application.command.delete;

import com.cyna.shared.application.Command;

import java.util.UUID;

/**
 * RGPD Art. 17 self-service erasure: the authenticated user deletes their own
 * account ({@code DELETE /api/v1/account}). {@code userId} is taken from the
 * security principal, never from the request body.
 */
public record DeleteMyAccountCommand(
        UUID userId
) implements Command<Void> {}
