package fr.dossierfacile.api.front.validator.annotation;

import fr.dossierfacile.api.front.validator.LengthOfTextValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {LengthOfTextValidator.class}
)
public @interface LengthOfText {
    String message() default "the number of words must be less than or equal to {max}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int max() default Integer.MAX_VALUE;
}