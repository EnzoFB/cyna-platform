package com.cyna.modules.user.interfaces.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateProfileRequestXssValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void should_reject_html_in_first_name() {
        var request = new UpdateProfileRequest(
                "<script>alert(1)</script>",
                "Dupont",
                "CYNA"
        );

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("First name must not contain HTML");
    }

    @Test
    void should_reject_html_in_last_name() {
        var request = new UpdateProfileRequest(
                "Alice",
                "<img src=x onerror=alert(1)>",
                "CYNA"
        );

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Last name must not contain HTML");
    }

    @Test
    void should_reject_html_in_company() {
        var request = new UpdateProfileRequest(
                "Alice",
                "Dupont",
                "<svg onload=alert(1)>"
        );

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Company must not contain HTML");
    }

    @Test
    void should_accept_request_without_html() {
        var request = new UpdateProfileRequest(
                "Alice",
                "Dupont",
                "CYNA SAS"
        );

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
