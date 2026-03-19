package com.cyna.modules.user.application.query.me;

import com.cyna.shared.application.Query;

import java.util.UUID;

public record GetCurrentUserQuery(
        UUID userId
) implements Query<UserReadModel> {}
