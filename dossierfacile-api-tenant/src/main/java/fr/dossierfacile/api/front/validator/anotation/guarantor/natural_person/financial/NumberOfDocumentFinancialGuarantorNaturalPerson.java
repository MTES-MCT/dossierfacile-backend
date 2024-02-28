package fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.financial;

import fr.dossierfacile.api.front.validator.guarantor.natural_person.financial.NumberOfDocumentFinancialGuarantorNaturalPersonValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {NumberOfDocumentFinancialGuarantorNaturalPersonValidator.class}
)

public @interface NumberOfDocumentFinancialGuarantorNaturalPerson {
    String message() default "you say you have no documents";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
