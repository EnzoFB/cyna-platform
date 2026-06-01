package com.cyna.shared.interfaces.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ContactRequestXssValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void should_reject_script_in_message() {
        var request = new ContactRequest(
                "Alice",
                "alice@example.com",
                "Question",
                "<script>alert('xss')</script>",
                "fr"
        );

        Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Message must not contain HTML");
    }

    @Test
    void should_reject_html_in_name() {
        var request = new ContactRequest(
                "<img src=x onerror=alert(1)>",
                "alice@example.com",
                "Subject",
                "Hello",
                "fr"
        );

        Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Name must not contain HTML");
    }

    @Test
    void should_reject_html_in_email() {
        var request = new ContactRequest(
                "Alice",
                "<a href='javascript:alert(1)'>click</a>@example.com",
                "Subject",
                "Hello",
                "fr"
        );

        Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Email must not contain HTML");
    }

    @Test
    void should_reject_html_in_subject() {
        var request = new ContactRequest(
                "Alice",
                "alice@example.com",
                "<svg onload=alert(1)>",
                "Hello",
                "fr"
        );

        Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Subject must not contain HTML");
    }

    @Test
    void should_accept_contact_request_without_html() {
        var request = new ContactRequest(
                "Alice Dupont",
                "alice@example.com",
                "Question sur mon abonnement",
                "Bonjour, j'aimerais savoir comment modifier mon abonnement. Merci.",
                "fr"
        );

        Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
