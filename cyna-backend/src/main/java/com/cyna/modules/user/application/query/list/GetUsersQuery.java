package com.cyna.modules.user.application.query.list;

import com.cyna.shared.application.Query;

public record GetUsersQuery(
        int page,
        int size
) implements Query<UsersPage> {}
