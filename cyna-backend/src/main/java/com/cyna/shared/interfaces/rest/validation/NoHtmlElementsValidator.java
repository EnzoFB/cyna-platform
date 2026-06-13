package com.cyna.shared.interfaces.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.regex.Pattern;

public class NoHtmlElementsValidator implements ConstraintValidator<NoHtmlElements, Collection<String>> {

    private static final Pattern HTML_MARKERS = Pattern.compile("<(?!=)|>(?!=)");

    @Override
    public boolean isValid(Collection<String> values, ConstraintValidatorContext context) {
        if (values == null) {
            return true;
        }
        for (String value : values) {
            if (value != null && HTML_MARKERS.matcher(value).find()) {
                return false;
            }
        }
        return true;
    }
}
