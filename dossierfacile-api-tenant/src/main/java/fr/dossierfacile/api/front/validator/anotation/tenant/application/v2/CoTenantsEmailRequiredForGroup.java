package fr.dossierfacile.api.front.validator.anotation.tenant.application.v2;

import fr.dossierfacile.api.front.validator.tenant.application.v2.CoTenantsEmailRequiredForGroupValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {CoTenantsEmailRequiredForGroupValidator.class}
)
public @interface CoTenantsEmailRequiredForGroup {
    String message() default "coTenant Should have email if group applicationType";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}