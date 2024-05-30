package fr.dossierfacile.api.front.validator.annotation;

import fr.dossierfacile.api.front.validator.DocumentSubcategorySubsetValidator;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {DocumentSubcategorySubsetValidator.class}
)
public @interface DocumentSubcategorySubset {
    DocumentSubCategory[] anyOf() default {};

    String message() default "must be one of {anyOf}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
