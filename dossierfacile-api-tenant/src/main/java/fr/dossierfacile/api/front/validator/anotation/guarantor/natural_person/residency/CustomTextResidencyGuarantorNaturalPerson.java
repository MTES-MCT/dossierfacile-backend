package fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.residency;

import fr.dossierfacile.api.front.validator.guarantor.natural_person.residency.CustomTextResidencyGuarantorNaturalPersonValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {CustomTextResidencyGuarantorNaturalPersonValidator.class}
)
public @interface CustomTextResidencyGuarantorNaturalPerson {
    String message() default "{javax.validation.constraints.NotBlank.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
