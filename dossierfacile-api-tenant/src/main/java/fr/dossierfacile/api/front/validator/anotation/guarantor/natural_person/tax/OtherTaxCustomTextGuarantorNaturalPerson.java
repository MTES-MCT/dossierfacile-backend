package fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.tax;

import fr.dossierfacile.api.front.validator.guarantor.natural_person.tax.OtherTaxCustomTextGuarantorNaturalPersonValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {OtherTaxCustomTextGuarantorNaturalPersonValidator.class}
)
public @interface OtherTaxCustomTextGuarantorNaturalPerson {
    String message() default "{javax.validation.constraints.NotBlank.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
