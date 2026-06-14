package com.cyna.shared.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoHtmlElementsValidatorTest {

    private final NoHtmlElementsValidator validator = new NoHtmlElementsValidator();

    @Test
    void should_accept_null_and_empty_list() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid(List.of(), null)).isTrue();
    }

    @Test
    void should_accept_plain_text_items() {
        assertThat(validator.isValid(List.of("24/7 SOC", "EDR included", "<= 100 endpoints"), null)).isTrue();
    }

    @Test
    void should_reject_list_with_html_item() {
        assertThat(validator.isValid(List.of("Valid point", "<script>alert(1)</script>"), null)).isFalse();
    }

    @Test
    void should_reject_list_with_html_in_any_item() {
        assertThat(validator.isValid(List.of("<img src=x onerror=alert(1)>"), null)).isFalse();
    }

    @Test
    void should_accept_list_with_null_element() {
        assertThat(validator.isValid(java.util.Arrays.asList(null, "valid"), null)).isTrue();
    }
}
