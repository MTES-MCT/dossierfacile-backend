package fr.dossierfacile.api.front.validator.annotation.tenant.financial;

import fr.dossierfacile.api.front.validator.tenant.financial.NoDocumentCustomTextFinancialValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {NoDocumentCustomTextFinancialValidator.class}
)
public @interface NoDocumentCustomTextFinancial {
    String message() default "{jakarta.validation.constraints.NotEmpty.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
