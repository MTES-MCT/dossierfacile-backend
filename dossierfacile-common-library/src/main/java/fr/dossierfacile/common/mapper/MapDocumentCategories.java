package fr.dossierfacile.common.mapper;

import org.mapstruct.Mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Mapping(target = "documentCategory", source = "documentCategory")
@Mapping(target = "documentSubCategory", source = "documentSubCategory")
// TODO Deprecated v5
@Mapping(target = "subCategory", source = "documentSubCategory")
@Mapping(target = "documentCategoryStep", source = "documentCategoryStep")
public @interface MapDocumentCategories {
}
