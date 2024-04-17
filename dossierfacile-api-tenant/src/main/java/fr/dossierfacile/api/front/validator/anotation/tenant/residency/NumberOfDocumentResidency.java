package fr.dossierfacile.api.front.validator.anotation.tenant.residency;

import fr.dossierfacile.api.front.validator.tenant.residency.NumberOfDocumentResidencyValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {NumberOfDocumentResidencyValidator.class}
)

public @interface NumberOfDocumentResidency {
    String message() default "number of documents for this type of residence is incorrect";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
