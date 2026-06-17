package com.cyna.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Server-side password strength constraint. Enforces the exact same policy as
 * the client (8+ chars, at least one lowercase, one uppercase, one digit and
 * one special character among {@code !@#$%^&*}), so a password accepted by the
 * UI is accepted by the API and vice versa. Defence in depth: the rule must not
 * live only in the front, where a direct API call could bypass it.
 *
 * <p>Null-tolerant by design (returns valid for {@code null}) so it composes
 * with {@link jakarta.validation.constraints.NotBlank} for presence checks,
 * mirroring {@link NoHtml}.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "Password must be at least 8 characters and include an uppercase letter, "
            + "a lowercase letter, a digit and a special character (!@#$%^&*)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
