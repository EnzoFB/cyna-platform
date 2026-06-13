package com.cyna.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generic anti-XSS constraint: rejects strings containing HTML markers.
 *
 * <p>Lives in the framework-neutral {@code shared.validation} package so it can
 * guard a value object at any boundary (request DTOs as well as application
 * payloads) without coupling a layer to {@code shared.interfaces}.
 */
@Documented
@Constraint(validatedBy = NoHtmlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoHtml {
    String message() default "HTML is not allowed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
