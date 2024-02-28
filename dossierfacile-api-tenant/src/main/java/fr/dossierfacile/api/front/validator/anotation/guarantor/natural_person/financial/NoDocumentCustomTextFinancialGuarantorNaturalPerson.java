package fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.financial;

import fr.dossierfacile.api.front.validator.guarantor.natural_person.financial.NoDocumentCustomTextFinancialGuarantorNaturalPersonValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {NoDocumentCustomTextFinancialGuarantorNaturalPersonValidator.class}
)
public @interface NoDocumentCustomTextFinancialGuarantorNaturalPerson {
    String message() default "{jakarta.validation.constraints.NotEmpty.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
