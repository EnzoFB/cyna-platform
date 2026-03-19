package com.cyna.modules.user.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void should_create_email_with_valid_format() {
        var email = Email.of("user@example.com");
        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @Test
    void should_reject_null_email() {
        assertThatThrownBy(() -> Email.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_blank_email() {
        assertThatThrownBy(() -> Email.of("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_email_without_at_sign() {
        assertThatThrownBy(() -> Email.of("userexample.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    void should_reject_email_without_domain() {
        assertThatThrownBy(() -> Email.of("user@"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }
}
