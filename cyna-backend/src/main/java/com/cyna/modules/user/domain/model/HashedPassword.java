package com.cyna.modules.user.domain.model;

import com.cyna.shared.domain.Guard;

public record HashedPassword(String value) {

    public HashedPassword {
        Guard.againstNullOrBlank(value, "HashedPassword.value");
    }

    public static HashedPassword of(String hash) {
        return new HashedPassword(hash);
    }
}
