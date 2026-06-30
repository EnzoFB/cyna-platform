package com.cyna.modules.user.domain.model;

import com.cyna.modules.user.domain.event.UserEmailVerified;
import com.cyna.shared.domain.Result;
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
        // Self-service registration now starts pending email verification.
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void register_raises_no_event_and_account_is_pending() {
        // Registration no longer raises a domain event: the welcome email is
        // deferred to activation, and the verification token/event are produced
        // by the application handler (which alone holds the raw token).
        var user = User.register(
                Email.of("test@example.com"),
                HashedPassword.of("hashed123"),
                "John", "Doe", "fr"
        );

        assertThat(user.getDomainEvents()).isEmpty();
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
    }

    @Test
    void activate_transitions_pending_to_active_and_raises_email_verified() {
        var user = User.register(
                Email.of("verify@example.com"),
                HashedPassword.of("hashed123"),
                "John", "Doe", "fr"
        );

        Result<User> result = user.activate("fr");

        assertThat(result.isSuccess()).isTrue();
        var activated = result.getValue();
        assertThat(activated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(activated.getDomainEvents()).hasSize(1);
        assertThat(activated.getDomainEvents().getFirst()).isInstanceOf(UserEmailVerified.class);

        var event = (UserEmailVerified) activated.getDomainEvents().getFirst();
        assertThat(event.userId()).isEqualTo(user.getId());
        assertThat(event.email()).isEqualTo("verify@example.com");
    }

    @Test
    void activate_rejects_a_non_pending_account() {
        var user = User.reconstitute(
                java.util.UUID.randomUUID(), Email.of("active@example.com"),
                HashedPassword.of("hash"), "John", "Doe", null,
                Role.CUSTOMER, UserStatus.ACTIVE, java.time.Instant.now(), java.time.Instant.now()
        );

        assertThat(user.activate("fr").isFailure()).isTrue();
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
