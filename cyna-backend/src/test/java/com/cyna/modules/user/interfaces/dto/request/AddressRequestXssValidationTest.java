package com.cyna.modules.user.interfaces.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AddressRequestXssValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void should_reject_html_in_address_field() {
        var request = new AddressRequest(
                "Alice", "Dupont", "Home",
                "<script>alert(1)</script>", null,
                "75001", "Paris", "IDF", "FR",
                "+33612345678", "CYNA", null
        );

        Set<ConstraintViolation<AddressRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Address must not contain HTML");
    }

    @Test
    void should_reject_html_in_city_field() {
        var request = new AddressRequest(
                "Alice", "Dupont", "Home",
                "1 rue de la Paix", null,
                "75001", "<img src=x onerror=alert(1)>", "IDF", "FR",
                "+33612345678", "CYNA", null
        );

        Set<ConstraintViolation<AddressRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("City must not contain HTML");
    }

    @Test
    void should_accept_address_without_html() {
        var request = new AddressRequest(
                "Alice", "Dupont", "Home",
                "1 rue de la Paix", "Batiment B",
                "75001", "Paris", "IDF", "FR",
                "+33612345678", "CYNA", "FR12345678901"
        );

        Set<ConstraintViolation<AddressRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
