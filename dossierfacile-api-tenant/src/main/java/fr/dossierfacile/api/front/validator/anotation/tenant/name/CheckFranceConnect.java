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
    String message() default "you can't edit your names that come from france connect";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
