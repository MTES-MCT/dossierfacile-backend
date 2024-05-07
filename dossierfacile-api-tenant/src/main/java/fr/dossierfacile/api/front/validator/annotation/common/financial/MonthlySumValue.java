package fr.dossierfacile.api.front.validator.annotation.common.financial;

import fr.dossierfacile.api.front.validator.common.financial.MonthlySumValueValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {MonthlySumValueValidator.class}
)

public @interface MonthlySumValue {
    String message() default "Montant mensuel doit être strictement positif";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
