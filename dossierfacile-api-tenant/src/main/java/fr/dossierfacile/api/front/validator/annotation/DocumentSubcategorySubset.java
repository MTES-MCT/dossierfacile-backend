package fr.dossierfacile.api.front.validator.annotation;

import fr.dossierfacile.api.front.validator.DocumentSubcategorySubsetValidator;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(DocumentSubcategorySubsets.class)
@Constraint(
        validatedBy = {DocumentSubcategorySubsetValidator.class}
)
public @interface DocumentSubcategorySubset {
    DocumentSubCategory[] anyOf() default {};

    String message() default "must be one of {anyOf}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
