package com.cyna.shared.interfaces.rest.validation;

import org.junit.jupiter.api.Test;

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
    void should_reject_html_markers() {
        assertThat(validator.isValid("<b>bold</b>", null)).isFalse();
        assertThat(validator.isValid("hello >", null)).isFalse();
        assertThat(validator.isValid("1 < 2", null)).isFalse();
    }
}
