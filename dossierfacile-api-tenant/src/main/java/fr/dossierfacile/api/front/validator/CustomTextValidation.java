package fr.dossierfacile.api.front.validator;

import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Shared rejection for document forms whose explanation text is mandatory in a given situation
 * (tax "other situation", financial "no document", guarantor residency "other").
 * Each validator keeps its own triggering condition and delegates the violation to this helper.
 */
public final class CustomTextValidation {

    public static final String BLANK_CUSTOM_TEXT_MESSAGE = "{jakarta.validation.constraints.NotBlank.message}";
    private static final String CUSTOM_TEXT_PROPERTY = "customText";

    private CustomTextValidation() {}

    /**
     * @return true when the text is present, otherwise attaches a NotBlank violation on customText and returns false
     */
    public static boolean requireCustomText(String customText, ConstraintValidatorContext constraintValidatorContext) {
        if (StringUtils.isNotBlank(customText)) {
            return true;
        }
        constraintValidatorContext.disableDefaultConstraintViolation();
        constraintValidatorContext
                .buildConstraintViolationWithTemplate(BLANK_CUSTOM_TEXT_MESSAGE)
                .addPropertyNode(CUSTOM_TEXT_PROPERTY).addConstraintViolation();
        return false;
    }
}
