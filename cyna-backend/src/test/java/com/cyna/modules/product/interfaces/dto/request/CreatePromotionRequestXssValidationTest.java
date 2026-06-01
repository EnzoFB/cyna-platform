package com.cyna.modules.product.interfaces.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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
    void should_reject_html_in_marketing_text_fr() {
        var request = new CreatePromotionRequest(
                UUID.randomUUID(), 10,
                "<script>alert(1)</script>",
                "Valid English text",
                Instant.now(), Instant.now().plusSeconds(3600),
                true, false, 1
        );

        Set<ConstraintViolation<CreatePromotionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Marketing text (FR) must not contain HTML");
    }

    @Test
    void should_reject_html_in_marketing_text_en() {
        var request = new CreatePromotionRequest(
                UUID.randomUUID(), 10,
                "Texte FR valide",
                "<img src=x onerror=alert(1)>",
                Instant.now(), Instant.now().plusSeconds(3600),
                true, false, 1
        );

        Set<ConstraintViolation<CreatePromotionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Marketing text (EN) must not contain HTML");
    }

    @Test
    void should_accept_promotion_without_html() {
        var request = new CreatePromotionRequest(
                UUID.randomUUID(), 10,
                "Offre speciale ete",
                "Summer special offer",
                Instant.now(), Instant.now().plusSeconds(3600),
                true, false, 1
        );

        Set<ConstraintViolation<CreatePromotionRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
