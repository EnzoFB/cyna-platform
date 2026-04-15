package com.cyna.modules.user.application.query.list;

import java.util.List;

public record UsersPage(
        List<UserListReadModel> items,
        long totalElements,
        int totalPages
) {}
