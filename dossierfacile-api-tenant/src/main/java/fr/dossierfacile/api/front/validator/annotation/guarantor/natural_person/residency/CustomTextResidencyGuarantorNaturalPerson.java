package fr.dossierfacile.api.front.validator.annotation.guarantor.natural_person.residency;

import fr.dossierfacile.api.front.validator.guarantor.natural_person.residency.CustomTextResidencyGuarantorNaturalPersonValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

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
    String message() default "{jakarta.validation.constraints.NotBlank.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
