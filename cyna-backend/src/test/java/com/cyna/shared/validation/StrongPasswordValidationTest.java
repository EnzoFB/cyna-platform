package com.cyna.shared.validation;

import com.cyna.modules.user.interfaces.dto.request.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the server-side password strength rule is actually enforced at the
 * DTO boundary (defence in depth — the client rule must not be the only gate).
 */
class StrongPasswordValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private static RegisterRequest withPassword(String password) {
        return new RegisterRequest(
                "user@example.com", password, "Alice", "Martin", "Acme", "fr", true);
    }

    private static boolean hasPasswordViolation(RegisterRequest request) {
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        return violations.stream().anyMatch(v -> "password".equals(v.getPropertyPath().toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Str0ng!Pass",   // upper + lower + digit + special
            "Aa1!aaaa",      // exactly 8 chars, all classes
            "MyP@ssw0rd123"
    })
    void should_accept_strong_passwords(String password) {
        assertThat(hasPasswordViolation(withPassword(password))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short",          // too short + missing classes
            "password123",    // no uppercase, no special
            "Password123",    // no special character
            "Password!!!",    // no digit
            "PASSWORD123!",   // no lowercase
            "Aa1!aaa"         // only 7 chars
    })
    void should_reject_weak_passwords(String password) {
        assertThat(hasPasswordViolation(withPassword(password))).isTrue();
    }
}
