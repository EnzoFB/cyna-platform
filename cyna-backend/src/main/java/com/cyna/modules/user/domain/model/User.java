package com.cyna.modules.user.domain.model;

import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Result;
import com.cyna.modules.user.domain.event.UserRegistered;
import com.cyna.modules.user.domain.event.UserDeactivated;

import java.time.Instant;
import java.util.UUID;

public class User extends AggregateRoot<UUID> {

    private final Email email;
    private final HashedPassword hashedPassword;
    private final String firstName;
    private final String lastName;
    private final Role role;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(UUID id, Email email, HashedPassword hashedPassword,
                 String firstName, String lastName, Role role, UserStatus status,
                 Instant createdAt, Instant updatedAt) {
        super(id);
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User register(Email email, HashedPassword hashedPassword,
                                String firstName, String lastName, String lang) {
        Guard.againstNull(email, "email");
        Guard.againstNull(hashedPassword, "hashedPassword");
        Guard.againstNullOrBlank(firstName, "firstName");
        Guard.againstNullOrBlank(lastName, "lastName");

        var now = Instant.now();
        var user = new User(
                UUID.randomUUID(), email, hashedPassword,
                firstName, lastName, Role.CUSTOMER, UserStatus.ACTIVE,
                now, now
        );

        user.raise(new UserRegistered(
                user.getId(),
                email.value(),
                user.getFirstName(),
                Role.CUSTOMER.name(),
                lang,
                now
        ));

        return user;
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
                firstName, lastName, role, UserStatus.ACTIVE,
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
                firstName, lastName, role, status,
                this.createdAt, Instant.now()
        );
    }

    public Result<User> deactivate(String reason) {
        if (this.status == UserStatus.INACTIVE) {
            return Result.failure("User is already inactive");
        }

        var deactivated = new User(
                this.getId(), this.email, this.hashedPassword,
                this.firstName, this.lastName, this.role, UserStatus.INACTIVE,
                this.createdAt, Instant.now()
        );

        deactivated.raise(new UserDeactivated(this.getId(), reason, Instant.now()));

        return Result.success(deactivated);
    }

    public static User reconstitute(UUID id, Email email, HashedPassword hashedPassword,
                                    String firstName, String lastName, Role role, UserStatus status,
                                    Instant createdAt, Instant updatedAt) {
        return new User(id, email, hashedPassword, firstName, lastName, role, status, createdAt, updatedAt);
    }

    public Email getEmail() { return email; }
    public HashedPassword getHashedPassword() { return hashedPassword; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
