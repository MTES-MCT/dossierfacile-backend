package fr.dossierfacile.api.front.validator.anotation.tenant.residency;

import fr.dossierfacile.api.front.validator.tenant.residency.CustomTextResidencyValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {CustomTextResidencyValidator.class}
)
public @interface CustomTextResidency {
    String message() default "{javax.validation.constraints.NotBlank.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
