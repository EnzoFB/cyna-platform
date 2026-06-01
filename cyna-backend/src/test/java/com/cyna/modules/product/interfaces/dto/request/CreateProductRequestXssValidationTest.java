package com.cyna.modules.product.interfaces.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreateProductRequestXssValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void should_reject_html_in_highlight_points() {
        var request = new CreateProductRequest(
                "Product Name",
                UUID.randomUUID(),
                1,
                "Service desc",
                "Technical desc",
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100),
                "EUR",
                14,
                List.of("Valid point", "<script>alert(1)</script>")
        );

        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Highlight points must not contain HTML");
    }

    @Test
    void should_accept_product_without_html_in_highlight_points() {
        var request = new CreateProductRequest(
                "Product Name",
                UUID.randomUUID(),
                1,
                "Service desc",
                "Technical desc",
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100),
                "EUR",
                14,
                List.of("24/7 SOC", "EDR included")
        );

        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
