package com.cyna.modules.product.interfaces.dto.request;

import com.cyna.modules.product.domain.model.ProductTranslation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    void should_reject_html_in_product_translation_name() {
        var request = new CreateProductRequest(
                Map.of("fr", new ProductTranslation(
                        "<script>alert(1)</script>",
                        "Service desc",
                        "Technical desc",
                        List.of("Valid point")
                )),
                UUID.randomUUID(),
                1,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100),
                "EUR",
                14
        );

        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Product name must not contain HTML");
    }

    @Test
    void should_reject_html_in_product_translation_highlight_points() {
        var request = new CreateProductRequest(
                Map.of("fr", new ProductTranslation(
                        "Product Name",
                        "Service desc",
                        "Technical desc",
                        List.of("<script>alert(1)</script>")
                )),
                UUID.randomUUID(),
                1,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100),
                "EUR",
                14
        );

        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Highlight points must not contain HTML");
    }

    @Test
    void should_accept_product_without_html() {
        var request = new CreateProductRequest(
                Map.of("fr", new ProductTranslation(
                        "Product Name",
                        "Service desc",
                        "Technical desc",
                        List.of("24/7 SOC", "EDR included")
                )),
                UUID.randomUUID(),
                1,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(100),
                "EUR",
                14
        );

        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
