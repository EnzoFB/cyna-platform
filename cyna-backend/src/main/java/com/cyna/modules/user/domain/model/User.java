package com.cyna.modules.user.domain.model;

import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Result;
import com.cyna.modules.user.domain.event.UserAnonymized;
import com.cyna.modules.user.domain.event.UserEmailChanged;
import com.cyna.modules.user.domain.event.UserEmailVerified;
import com.cyna.modules.user.domain.event.UserPasswordChanged;
import com.cyna.modules.user.domain.event.UserRegistered;
import com.cyna.modules.user.domain.event.UserDeactivated;

import java.time.Instant;
import java.util.UUID;

public class User extends AggregateRoot<UUID> {

    private final Email email;
    private final HashedPassword hashedPassword;
    private final String firstName;
    private final String lastName;
    private final String company;
    private final Role role;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(UUID id, Email email, HashedPassword hashedPassword,
                 String firstName, String lastName, String company, Role role, UserStatus status,
                 Instant createdAt, Instant updatedAt) {
        super(id);
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.firstName = firstName;
        this.lastName = lastName;
        this.company = company;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Self-service registration without a company (kept for callers/tests that don't supply one). */
    public static User register(Email email, HashedPassword hashedPassword,
                                String firstName, String lastName, String lang) {
        return register(email, hashedPassword, firstName, lastName, null, lang);
    }

    /**
     * Self-service registration. The account is created in
     * {@link UserStatus#PENDING_VERIFICATION} and cannot authenticate until the
     * user confirms their email via the verification link (see {@link #activate}).
     *
     * <p>No domain event is raised here: registration no longer sends a welcome
     * email (that is deferred to {@link #activate()} via {@code UserEmailVerified}),
     * and the email-verification token + its {@code EmailVerificationRequested}
     * event are produced by the application handler, which alone holds the raw
     * token.</p>
     */
    public static User register(Email email, HashedPassword hashedPassword,
                                String firstName, String lastName, String company, String lang) {
        Guard.againstNull(email, "email");
        Guard.againstNull(hashedPassword, "hashedPassword");
        Guard.againstNullOrBlank(firstName, "firstName");
        Guard.againstNullOrBlank(lastName, "lastName");

        var now = Instant.now();
        return new User(
                UUID.randomUUID(), email, hashedPassword,
                firstName, lastName, company, Role.CUSTOMER, UserStatus.PENDING_VERIFICATION,
                now, now
        );
    }

    /**
     * Confirms the user's email and activates the account. Valid only from
     * {@link UserStatus#PENDING_VERIFICATION}; any other state is rejected so a
     * consumed/late verification link cannot reactivate a deactivated or
     * anonymized account. Raises {@link UserEmailVerified} so the welcome email
     * is sent exactly once, on first activation.
     */
    public Result<User> activate(String lang) {
        if (this.status != UserStatus.PENDING_VERIFICATION) {
            return Result.failure("Account is not pending verification");
        }
        var now = Instant.now();
        var activated = new User(
                this.getId(), this.email, this.hashedPassword,
                this.firstName, this.lastName, this.company, this.role, UserStatus.ACTIVE,
                this.createdAt, now
        );
        activated.raise(new UserEmailVerified(
                this.getId(), this.email.value(), this.firstName, lang, now
        ));
        return Result.success(activated);
    }

    public static User createByAdmin(Email email, HashedPassword hashedPassword,
                                     String firstName, String lastName, Role role) {
        Guard.againstNull(email, "email");
        Guard.againstNull(hashedPassword, "hashedPassword");
        Guard.againstNullOrBlank(firstName, "firstName");
        Guard.againstNullOrBlank(lastName, "lastName");
        Guard.againstNull(role, "role");

        var now = Instant.now();
        var user = new User(
                UUID.randomUUID(), email, hashedPassword,
                firstName, lastName, null, role, UserStatus.ACTIVE,
                now, now
        );

        user.raise(new UserRegistered(
                user.getId(),
                email.value(),
                firstName,
                role.name(),
                "fr",
                now
        ));

        return user;
    }

    public User updateInfo(String firstName, String lastName, Role role, UserStatus status) {
        Guard.againstNullOrBlank(firstName, "firstName");
        Guard.againstNullOrBlank(lastName, "lastName");
        Guard.againstNull(role, "role");
        Guard.againstNull(status, "status");

        return new User(
                this.getId(), this.email, this.hashedPassword,
                firstName, lastName, this.company, role, status,
                this.createdAt, Instant.now()
        );
    }

    public User updateProfile(String firstName, String lastName, String company) {
        String newFirstName = (firstName != null && !firstName.isBlank()) ? firstName : this.firstName;
        String newLastName  = (lastName  != null && !lastName.isBlank())  ? lastName  : this.lastName;
        return new User(
                this.getId(), this.email, this.hashedPassword,
                newFirstName, newLastName, company,
                this.role, this.status,
                this.createdAt, Instant.now()
        );
    }

    public User changePassword(HashedPassword newHashedPassword, String lang) {
        Guard.againstNull(newHashedPassword, "newHashedPassword");
        var now = Instant.now();
        var updated = new User(
                this.getId(), this.email, newHashedPassword,
                this.firstName, this.lastName, this.company,
                this.role, this.status,
                this.createdAt, now
        );
        updated.raise(new UserPasswordChanged(
                this.getId(), this.email.value(), this.firstName, lang, now
        ));
        return updated;
    }

    public User withEmail(Email newEmail, String lang) {
        Guard.againstNull(newEmail, "newEmail");
        var now = Instant.now();
        var updated = new User(
                this.getId(), newEmail, this.hashedPassword,
                this.firstName, this.lastName, this.company,
                this.role, this.status,
                this.createdAt, now
        );
        updated.raise(new UserEmailChanged(
                this.getId(), this.email.value(), newEmail.value(),
                this.firstName, lang, now
        ));
        return updated;
    }

    public Result<User> deactivate(String reason) {
        if (this.status == UserStatus.INACTIVE) {
            return Result.failure("User is already inactive");
        }

        var deactivated = new User(
                this.getId(), this.email, this.hashedPassword,
                this.firstName, this.lastName, this.company, this.role, UserStatus.INACTIVE,
                this.createdAt, Instant.now()
        );

        deactivated.raise(new UserDeactivated(this.getId(), reason, Instant.now()));

        return Result.success(deactivated);
    }

    /**
     * RGPD Art. 17 — irreversible anonymization. Used when the account cannot
     * be hard-deleted because it carries a legally-retained transactional
     * footprint (orders/invoices: French Code de commerce L123-22 = 10 years).
     * Every direct identifier is overwritten with a non-identifying value, the
     * password is replaced by a non-matchable sentinel, and the status becomes
     * {@link UserStatus#ANONYMIZED} (login is refused for any non-ACTIVE
     * account). The surrogate {@code id} is preserved so the retained
     * accounting records keep a valid — but no longer personal — FK anchor.
     *
     * <p>Idempotent: re-anonymizing an already-anonymized account is a no-op
     * that raises no event.
     */
    public User anonymize() {
        if (this.status == UserStatus.ANONYMIZED) {
            return this;
        }
        var now = Instant.now();
        var anonymized = new User(
                this.getId(),
                Email.of("anonymized+" + this.getId() + "@deleted.invalid"),
                HashedPassword.of("ANONYMIZED-NO-LOGIN"),
                "Compte", "supprimé", null,
                this.role, UserStatus.ANONYMIZED,
                this.createdAt, now
        );
        anonymized.raise(new UserAnonymized(this.getId(), now));
        return anonymized;
    }

    public static User reconstitute(UUID id, Email email, HashedPassword hashedPassword,
                                    String firstName, String lastName, String company, Role role, UserStatus status,
                                    Instant createdAt, Instant updatedAt) {
        return new User(id, email, hashedPassword, firstName, lastName, company, role, status, createdAt, updatedAt);
    }

    public Email getEmail() { return email; }
    public HashedPassword getHashedPassword() { return hashedPassword; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getCompany() { return company; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
