package fr.dossierfacile.api.front.validator.anotation;

import fr.dossierfacile.api.front.validator.NumberOfPagesValidator;
import fr.dossierfacile.common.enums.DocumentCategory;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {NumberOfPagesValidator.class}
)
public @interface NumberOfPages {
    String message() default "number of pages of the document must be less than or equal to {max}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    DocumentCategory category() default DocumentCategory.IDENTIFICATION;

    int max() default 2147483647;
}