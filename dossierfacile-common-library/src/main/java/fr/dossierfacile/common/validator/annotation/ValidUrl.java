package fr.dossierfacile.common.validator.annotation;

import fr.dossierfacile.common.validator.ValidUrlValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, METHOD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidUrlValidator.class)
@Documented
public @interface ValidUrl {
    String message() default "Invalid URL or internal/private host not allowed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
