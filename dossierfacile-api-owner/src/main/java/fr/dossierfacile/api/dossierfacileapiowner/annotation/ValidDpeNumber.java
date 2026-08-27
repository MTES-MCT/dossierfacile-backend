package fr.dossierfacile.api.dossierfacileapiowner.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ValidDpeNumberValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDpeNumber {

    String message() default "DPE number must consist of 13 alphanumeric characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
