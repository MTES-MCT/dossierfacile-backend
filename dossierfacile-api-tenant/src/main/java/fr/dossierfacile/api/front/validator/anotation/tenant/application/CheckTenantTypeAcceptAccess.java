package fr.dossierfacile.api.front.validator.anotation.tenant.application;

import fr.dossierfacile.api.front.validator.tenant.application.CheckTenantTypeAcceptAccessValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Deprecated
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {CheckTenantTypeAcceptAccessValidator.class}
)
public @interface CheckTenantTypeAcceptAccess {
    String message() default "accept access must not null";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
