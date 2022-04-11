package fr.gouv.owner.annotation;

import fr.gouv.owner.validator.JoinApartmentSharingCapacityValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@Constraint(
        validatedBy = {JoinApartmentSharingCapacityValidator.class}
)

public @interface JoinApartmentSharingCapacity {

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
