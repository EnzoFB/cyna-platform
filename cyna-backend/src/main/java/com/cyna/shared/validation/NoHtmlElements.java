package com.cyna.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generic anti-XSS constraint for collections of strings: rejects any element
 * containing HTML markers. See {@link NoHtml}.
 */
@Documented
@Constraint(validatedBy = NoHtmlElementsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoHtmlElements {
    String message() default "List items must not contain HTML";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
