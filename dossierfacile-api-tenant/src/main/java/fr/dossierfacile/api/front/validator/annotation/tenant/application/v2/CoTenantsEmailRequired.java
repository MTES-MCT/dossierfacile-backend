package fr.dossierfacile.api.front.validator.annotation.tenant.application.v2;

import fr.dossierfacile.api.front.validator.tenant.application.CoTenantsEmailRequiredValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {CoTenantsEmailRequiredValidator.class}
)
public @interface CoTenantsEmailRequired {
    String message() default "coTenant should have an email for a group or couple applicationType";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}