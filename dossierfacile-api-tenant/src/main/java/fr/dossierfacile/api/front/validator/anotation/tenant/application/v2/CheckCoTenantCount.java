package fr.dossierfacile.api.front.validator.anotation.tenant.application.v2;

import fr.dossierfacile.api.front.validator.tenant.application.v2.CheckCoTenantCountValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {CheckCoTenantCountValidator.class}
)
public @interface CheckCoTenantCount {
    String message() default "there is no correspondence between the type of application and the number of co-tenants";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

