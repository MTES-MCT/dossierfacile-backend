package fr.dossierfacile.api.front.validator.anotation.tenant.application;

import fr.dossierfacile.api.front.validator.tenant.application.DistinctEmailListValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {DistinctEmailListValidator.class}
)

public @interface DistinctEmailList {
    String message() default "the emails must be different";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
