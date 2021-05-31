package fr.dossierfacile.api.front.validator.anotation.tenant;

import fr.dossierfacile.api.front.validator.tenant.NumberOfPagesValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {NumberOfPagesValidator.class}
)
public @interface NumberOfPages {
    String message() default "number of pages must be less than or equal to {max}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int max() default 2147483647;
}