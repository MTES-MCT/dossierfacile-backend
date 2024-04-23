package fr.dossierfacile.api.front.validator.annotation.tenant.application.v2;

import fr.dossierfacile.api.front.validator.tenant.application.DistinctCoTenantEmailListValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {DistinctCoTenantEmailListValidator.class}
)
public @interface DistinctCoTenantEmailList {
    String message() default "the emails must be different";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
