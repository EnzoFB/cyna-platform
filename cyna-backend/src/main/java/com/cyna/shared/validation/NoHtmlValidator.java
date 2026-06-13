package com.cyna.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {

    private static final Pattern HTML_MARKERS = Pattern.compile("<(?!=)|>(?!=)");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return !HTML_MARKERS.matcher(value).find();
    }
}
