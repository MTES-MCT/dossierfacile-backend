package fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person;

import fr.dossierfacile.api.front.validator.guarantor.ExistGuarantorValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {ExistGuarantorValidator.class}
)

public @interface ExistGuarantor {
    String message() default "the guarantor does not exist";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
