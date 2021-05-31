package fr.dossierfacile.api.front.validator.anotation.tenant.tax;

import fr.dossierfacile.api.front.validator.tenant.tax.OtherTaxCustomTextValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
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
    String message() default "{javax.validation.constraints.NotBlank.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
