package com.cyna.modules.user.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void should_generate_unique_ids() {
        var id1 = UserId.generate();
        var id2 = UserId.generate();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void should_wrap_existing_uuid() {
        var uuid = UUID.randomUUID();
        var userId = UserId.of(uuid);
        assertThat(userId.value()).isEqualTo(uuid);
    }

    @Test
    void should_reject_null_value() {
        assertThatThrownBy(() -> UserId.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
