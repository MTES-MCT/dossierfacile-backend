package fr.dossierfacile.api.front.validator.anotation.tenant.type_guarantor;

import fr.dossierfacile.api.front.validator.tenant.type_guarantor.MaxGuarantorValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {MaxGuarantorValidator.class}
)

public @interface MaxGuarantor {
    String message() default "you have problem with your guarantors, you only have one organism guarantor or " +
            "one legal person guarantor or two natural personal guarantor";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
