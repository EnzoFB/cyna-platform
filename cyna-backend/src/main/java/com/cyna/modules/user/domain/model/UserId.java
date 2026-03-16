package com.cyna.modules.user.domain.model;

import com.cyna.shared.domain.Guard;

import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Guard.againstNull(value, "UserId.value");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }
}
