package fr.dossierfacile.common.validator.annotation;

import fr.dossierfacile.common.validator.SizeFileValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * OWASP File Upload — "Set a file size limit".
 * <p>
 * Validates that each uploaded file does not exceed the specified size in megabytes.
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SizeFileValidator.class)
public @interface SizeFile {

    String message() default "size must be less than or equal to {max} Mo";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    double max() default Double.MAX_VALUE;
}
