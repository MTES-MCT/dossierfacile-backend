package fr.dossierfacile.api.front.validator.anotation.tenant.name;

import fr.dossierfacile.api.front.validator.tenant.name.CheckFranceConnectValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {CheckFranceConnectValidator.class}
)

public @interface CheckFranceConnect {
    String message() default "You cannot edit your firstname or lastname when the account is linked to a FranceConnect Account";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
