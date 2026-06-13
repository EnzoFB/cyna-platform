package com.cyna.modules.account.application.command.delete;

import com.cyna.shared.application.Command;

import java.util.UUID;

/**
 * Admin-initiated erasure ({@code DELETE /api/v1/admin/users/{id}}).
 */
public record DeleteUserCommand(
        UUID userId
) implements Command<Void> {}
