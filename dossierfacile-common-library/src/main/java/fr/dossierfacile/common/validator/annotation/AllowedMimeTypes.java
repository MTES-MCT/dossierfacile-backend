package fr.dossierfacile.common.validator.annotation;

import fr.dossierfacile.common.validator.AllowedMimeTypesValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * OWASP File Upload — "List allowed extensions" + "Validate the file type, don't trust the Content-Type header"
 * + "Ensure that input validation is applied before validating the extensions".
 * <p>
 * Validates that each file's actual MIME type (detected via magic bytes with Apache Tika)
 * is in the allowed list. Does not rely on the client-supplied Content-Type header.
 */
@Target({ElementType.FIELD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AllowedMimeTypesValidator.class)
public @interface AllowedMimeTypes {

    String message() default "invalid file type";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Allowed MIME types (e.g. "application/pdf", "image/jpeg", "image/png", "image/heif").
     */
    String[] value();
}
