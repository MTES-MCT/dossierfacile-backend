package fr.dossierfacile.api.front.validator.anotation.guarantor;

import fr.dossierfacile.api.front.validator.guarantor.NumberOfDocumentGuarantorValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {NumberOfDocumentGuarantorValidator.class}
)

public @interface NumberOfDocumentGuarantor {
    String message() default "number of document must be between {min} and {max}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int min() default 1;

    int max() default 2147483647;
}
