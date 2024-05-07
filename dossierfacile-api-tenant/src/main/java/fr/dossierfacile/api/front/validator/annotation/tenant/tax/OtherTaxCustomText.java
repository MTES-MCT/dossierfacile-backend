package fr.dossierfacile.api.front.validator.annotation.tenant.tax;

import fr.dossierfacile.api.front.validator.tenant.tax.OtherTaxCustomTextValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {OtherTaxCustomTextValidator.class}
)
public @interface OtherTaxCustomText {
    String message() default "{jakarta.validation.constraints.NotBlank.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
