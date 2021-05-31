package fr.dossierfacile.api.front.validator.anotation.tenant.application;

import fr.dossierfacile.api.front.validator.tenant.application.UniqueEmailListCoTenantValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {UniqueEmailListCoTenantValidator.class}
)
public @interface UniqueEmailListCoTenant {

    String message() default "the emails are already being used";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
