package fr.dossierfacile.api.front.validator.annotation;

import fr.dossierfacile.api.front.validator.DocumentCategoryStepSubsetValidator;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {DocumentCategoryStepSubsetValidator.class}
)
public @interface DocumentCategoryStepSubset {
    DocumentCategoryStep[] anyOf() default {};

    String message() default "must be one of {anyOf}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
