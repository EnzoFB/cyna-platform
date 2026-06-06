package com.cyna.shared.interfaces.rest.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NoHtmlValidatorTest {

    private final NoHtmlValidator validator = new NoHtmlValidator();

    @Test
    void should_accept_null_and_plain_text() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("Plain text", null)).isTrue();
        assertThat(validator.isValid("EDR for 3 agents", null)).isTrue();
    }

    @Test
    void should_accept_less_than_or_equal_operators() {
        assertThat(validator.isValid("Price <= 100", null)).isTrue();
        assertThat(validator.isValid("Price >= 50", null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<b>bold</b>",
        "hello >",
        "1 < 2",
        "<script>alert('xss')</script>",
        "<img src=x onerror=alert(1)>",
        "<svg onload=alert(1)>",
        "<iframe src=javascript:alert(1)>",
        "<body onload=alert(1)>",
        "javascript:alert(1) <script>",
        "test<div>injection</div>",
        "<a href='javascript:alert(1)'>click</a>"
    })
    void should_reject_html_markers(String maliciousInput) {
        assertThat(validator.isValid(maliciousInput, null))
                .withFailMessage("Expected rejection for: %s", maliciousInput)
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Appartement <= 5",
        "Quantite >= 10",
        "100% safe text",
        "Text with special chars: & # @ $ %",
        "Unicode: é à ü ñ 中文 🚀"
    })
    void should_accept_safe_text(String safeInput) {
        assertThat(validator.isValid(safeInput, null))
                .withFailMessage("Expected acceptance for: %s", safeInput)
                .isTrue();
    }
}
