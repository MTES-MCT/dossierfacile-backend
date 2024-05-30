package fr.dossierfacile.api.front.validator.annotation.tenant.tax;

import fr.dossierfacile.api.front.validator.tenant.tax.NumberOfDocumentTaxValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {NumberOfDocumentTaxValidator.class}
)

public @interface NumberOfDocumentTax {
    String message() default "numbers of documents incorrect";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
