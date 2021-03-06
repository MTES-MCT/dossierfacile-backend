package fr.dossierfacile.api.front.validator.anotation;

import fr.dossierfacile.api.front.validator.ExtensionValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {ExtensionValidator.class}
)
public @interface Extension {
    String message() default "invalid file extension";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
