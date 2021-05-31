package fr.dossierfacile.api.front.validator.anotation.tenant.tax;

import fr.dossierfacile.api.front.validator.tenant.tax.MyNameAcceptVerificationValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(
        validatedBy = {MyNameAcceptVerificationValidator.class}
)
public @interface MyNameAcceptVerification {
    String message() default "{javax.validation.constraints.AssertTrue.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
