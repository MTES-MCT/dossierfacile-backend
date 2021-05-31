package fr.dossierfacile.api.front.validator.anotation.tenant.application;

import fr.dossierfacile.api.front.validator.tenant.application.DistinctTenantPrincipalEmailListCoTenantValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {DistinctTenantPrincipalEmailListCoTenantValidator.class}
)
public @interface DistinctTenantPrincipalEmailListCoTenant {

    String message() default "the co-tenants' email must be different from the email of the main tenant";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
