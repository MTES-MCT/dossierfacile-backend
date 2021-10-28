package fr.dossierfacile.api.front.validator.anotation.tenant.honor_declaration;

import fr.dossierfacile.api.front.validator.tenant.honor_declaration.CheckHonorDeclarationClarificationValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {CheckHonorDeclarationClarificationValidator.class}
)
public @interface CheckHonorDeclarationClarification {
    String message() default "you can't create clarification field, only tenant create have access";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
