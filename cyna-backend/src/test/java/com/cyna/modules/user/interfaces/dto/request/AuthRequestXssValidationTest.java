package com.cyna.modules.user.interfaces.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRequestXssValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void should_reject_html_in_login_request_email() {
        var request = new LoginRequest(
                "<script>alert(1)</script>@example.com",
                "Password123!",
                "fr"
        );

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Email must not contain HTML");
    }

    @Test
    void should_reject_html_in_login_request_password() {
        var request = new LoginRequest(
                "user@example.com",
                "pass<script>123",
                "fr"
        );

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Password must not contain HTML");
    }

    @Test
    void should_reject_html_in_forgot_password_email() {
        var request = new ForgotPasswordRequest(
                "<img src=x onerror=alert(1)>@example.com",
                "fr"
        );

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Email must not contain HTML");
    }

    @Test
    void should_reject_html_in_reset_password_new_password() {
        var request = new ResetPasswordRequest(
                "valid-token-123",
                "newpass<script>"
        );

        Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Password must not contain HTML");
    }

    @Test
    void should_reject_html_in_change_password_current_password() {
        var request = new ChangePasswordRequest(
                "oldpass<script>",
                "newpass123"
        );

        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Current password must not contain HTML");
    }

    @Test
    void should_reject_html_in_request_email_change_new_email() {
        var request = new RequestEmailChangeRequest(
                "<svg onload=alert(1)>@example.com",
                "fr"
        );

        Set<ConstraintViolation<RequestEmailChangeRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Email must not contain HTML");
    }
}
