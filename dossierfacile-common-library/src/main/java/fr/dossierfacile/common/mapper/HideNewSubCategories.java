package fr.dossierfacile.common.mapper;

import org.mapstruct.Mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO remove this behavior 6 months from now
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Mapping(target = "documentSubCategory", expression = "java(document.getDocumentSubCategory().getOnlyOldCategories())")
public @interface HideNewSubCategories {
}
