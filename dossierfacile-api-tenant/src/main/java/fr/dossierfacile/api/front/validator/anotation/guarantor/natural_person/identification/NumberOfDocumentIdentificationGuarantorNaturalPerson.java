package fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.identification;

import fr.dossierfacile.api.front.validator.guarantor.natural_person.identification.NumberOfDocumentIdentificationGuarantorNaturalPersonValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {NumberOfDocumentIdentificationGuarantorNaturalPersonValidator.class}
)

public @interface NumberOfDocumentIdentificationGuarantorNaturalPerson {
    String message() default "number of document must be between 1 and 5";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
