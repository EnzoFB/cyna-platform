package com.cyna.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    // Mirrors the client-side rule exactly (auth/profile forms): 8+ chars with at
    // least one lowercase, one uppercase, one digit and one special character,
    // restricted to the [A-Za-z0-9!@#$%^&*] alphabet.
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return STRONG_PASSWORD.matcher(value).matches();
    }
}
