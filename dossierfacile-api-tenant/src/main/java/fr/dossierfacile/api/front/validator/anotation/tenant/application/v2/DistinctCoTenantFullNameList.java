package fr.dossierfacile.api.front.validator.anotation.tenant.application.v2;

import fr.dossierfacile.api.front.validator.tenant.application.v2.DistinctCoTenantFullNameValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {DistinctCoTenantFullNameValidator.class}
)
public @interface DistinctCoTenantFullNameList {
    String message() default "the firstName and LastName must be different";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
