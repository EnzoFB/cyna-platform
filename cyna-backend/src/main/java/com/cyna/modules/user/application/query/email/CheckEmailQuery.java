package com.cyna.modules.user.application.query.email;

import com.cyna.shared.application.Query;

public record CheckEmailQuery(String email) implements Query<Boolean> {}
