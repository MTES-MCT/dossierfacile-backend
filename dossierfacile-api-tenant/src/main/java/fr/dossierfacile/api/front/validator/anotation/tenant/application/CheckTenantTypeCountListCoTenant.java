package fr.dossierfacile.api.front.validator.anotation.tenant.application;

import fr.dossierfacile.api.front.validator.tenant.application.CheckTenantTypeCountListCoTenantValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {CheckTenantTypeCountListCoTenantValidator.class}
)
public @interface CheckTenantTypeCountListCoTenant {
    String message() default "there is no correspondence between the type of application and the number of co-tenants";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
