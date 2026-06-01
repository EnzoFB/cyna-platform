package com.cyna.modules.product.interfaces.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateOfferCarouselSettingsRequestXssValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void should_reject_html_in_fixed_text_fr() {
        var request = new UpdateOfferCarouselSettingsRequest(
                "<script>alert(1)</script>",
                "Valid EN"
        );

        Set<ConstraintViolation<UpdateOfferCarouselSettingsRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Fixed text (FR) must not contain HTML");
    }

    @Test
    void should_reject_html_in_fixed_text_en() {
        var request = new UpdateOfferCarouselSettingsRequest(
                "Valide FR",
                "<img src=x onerror=alert(1)>"
        );

        Set<ConstraintViolation<UpdateOfferCarouselSettingsRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Fixed text (EN) must not contain HTML");
    }

    @Test
    void should_accept_request_without_html() {
        var request = new UpdateOfferCarouselSettingsRequest(
                "Decouvrez nos offres",
                "Discover our offers"
        );

        Set<ConstraintViolation<UpdateOfferCarouselSettingsRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
