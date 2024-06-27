package fr.dossierfacile.api.front.validator.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DocumentSubcategorySubsets {
    DocumentSubcategorySubset[] value();
}
