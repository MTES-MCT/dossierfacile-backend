package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.validator.annotation.LengthOfText;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LengthOfTextValidator implements ConstraintValidator<LengthOfText, String> {

    private Integer max;

    @Override
    public void initialize(LengthOfText constraintAnnotation) {
        max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String customText, ConstraintValidatorContext constraintValidatorContext) {
        return null == customText || customText.length() <= max;
    }
}
