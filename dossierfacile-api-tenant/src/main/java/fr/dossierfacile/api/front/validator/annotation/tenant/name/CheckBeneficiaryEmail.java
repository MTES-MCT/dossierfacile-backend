package fr.dossierfacile.api.front.validator.annotation.tenant.name;

import fr.dossierfacile.api.front.validator.tenant.name.CheckBeneficiaryEmailValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {CheckBeneficiaryEmailValidator.class}
)

public @interface CheckBeneficiaryEmail {
    String message() default "The beneficiary email is required when the dossier is filled for a third party";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
