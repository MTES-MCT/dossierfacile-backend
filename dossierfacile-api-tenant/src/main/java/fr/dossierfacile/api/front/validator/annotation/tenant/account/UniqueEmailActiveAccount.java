package fr.dossierfacile.api.front.validator.annotation.tenant.account;

import fr.dossierfacile.api.front.validator.tenant.account.UniqueEmailActiveAccountValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {UniqueEmailActiveAccountValidator.class}
)

public @interface UniqueEmailActiveAccount {
    String message() default "the email is already in use or must be different from null";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
