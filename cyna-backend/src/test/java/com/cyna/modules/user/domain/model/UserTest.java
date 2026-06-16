package com.cyna.modules.user.domain.model;

import com.cyna.modules.user.domain.event.UserRegistered;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void should_register_user_with_customer_role() {
        var email = Email.of("test@example.com");
        var password = HashedPassword.of("hashed123");

        var user = User.register(email, password, "John", "Doe", "fr");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getHashedPassword()).isEqualTo(password);
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void should_raise_user_registered_event() {
        var user = User.register(
                Email.of("test@example.com"),
                HashedPassword.of("hashed123"),
                "John", "Doe", "fr"
        );

        assertThat(user.getDomainEvents()).hasSize(1);
        assertThat(user.getDomainEvents().getFirst()).isInstanceOf(UserRegistered.class);

        var event = (UserRegistered) user.getDomainEvents().getFirst();
        assertThat(event.userId()).isEqualTo(user.getId());
        assertThat(event.email()).isEqualTo("test@example.com");
        assertThat(event.role()).isEqualTo("CUSTOMER");
    }

    @Test
    void should_reject_null_email() {
        assertThatThrownBy(() -> User.register(null, HashedPassword.of("hash"), "John", "Doe", "fr"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_blank_first_name() {
        assertThatThrownBy(() -> User.register(Email.of("a@b.com"), HashedPassword.of("hash"), "", "Doe", "fr"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_blank_last_name() {
        assertThatThrownBy(() -> User.register(Email.of("a@b.com"), HashedPassword.of("hash"), "John", "", "fr"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reconstitute_without_events() {
        var user = User.reconstitute(
                java.util.UUID.randomUUID(), Email.of("test@example.com"),
                HashedPassword.of("hash"), "John", "Doe", null,
                Role.ADMIN, UserStatus.ACTIVE, java.time.Instant.now(), java.time.Instant.now()
        );

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.getDomainEvents()).isEmpty();
    }
}
