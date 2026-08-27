package fr.dossierfacile.api.dossierfacileapiowner.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class ValidDpeNumberValidator implements ConstraintValidator<ValidDpeNumber, String> {

    private static final Pattern DPE_NUMBER_PATTERN = Pattern.compile("^[a-zA-Z0-9]{13}$");

    @Override
    public boolean isValid(String dpeNumber, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(dpeNumber)) {
            return true;
        }
        return DPE_NUMBER_PATTERN.matcher(dpeNumber.trim()).matches();
    }
}
