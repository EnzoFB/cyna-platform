package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.modules.product.application.translation.PromotionTranslationDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreatePromotionRequestXssValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void should_reject_html_in_promotion_translation() {
        var request = new CreatePromotionRequest(
                UUID.randomUUID(), 10,
                Map.of("fr", new PromotionTranslationDto("<script>alert(1)</script>")),
                Instant.now(), Instant.now().plusSeconds(3600),
                true
        );

        Set<ConstraintViolation<CreatePromotionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Marketing text must not contain HTML");
    }

    @Test
    void should_accept_promotion_without_html() {
        var request = new CreatePromotionRequest(
                UUID.randomUUID(), 10,
                Map.of(
                        "fr", new PromotionTranslationDto("Offre speciale ete"),
                        "en", new PromotionTranslationDto("Summer special offer")
                ),
                Instant.now(), Instant.now().plusSeconds(3600),
                true
        );

        Set<ConstraintViolation<CreatePromotionRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
