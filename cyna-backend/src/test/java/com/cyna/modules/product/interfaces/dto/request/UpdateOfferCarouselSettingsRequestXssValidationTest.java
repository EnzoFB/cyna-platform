package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.modules.product.application.translation.CarouselSettingsTranslationDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
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
    void should_reject_html_in_carousel_translation() {
        var request = new UpdateOfferCarouselSettingsRequest(
                Map.of("fr", new CarouselSettingsTranslationDto("<script>alert(1)</script>")),
                5
        );

        Set<ConstraintViolation<UpdateOfferCarouselSettingsRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Fixed text must not contain HTML");
    }

    @Test
    void should_accept_request_without_html() {
        var request = new UpdateOfferCarouselSettingsRequest(
                Map.of(
                        "fr", new CarouselSettingsTranslationDto("Decouvrez nos offres"),
                        "en", new CarouselSettingsTranslationDto("Discover our offers")
                ),
                5
        );

        Set<ConstraintViolation<UpdateOfferCarouselSettingsRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
