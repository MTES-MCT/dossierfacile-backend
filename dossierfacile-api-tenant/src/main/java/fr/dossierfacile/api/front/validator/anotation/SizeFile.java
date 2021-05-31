package fr.dossierfacile.api.front.validator.anotation;

import fr.dossierfacile.api.front.validator.SizeFileValidator;
import fr.dossierfacile.api.front.validator.enums.TypeDocumentValidation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {SizeFileValidator.class}
)
public @interface SizeFile {
    String message() default "size must be less than or equal to {max} Mo";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    double max() default Double.MAX_VALUE;

    TypeDocumentValidation typeDocumentValidation();
}