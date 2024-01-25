package fr.dossierfacile.common.mapper;

import org.mapstruct.Mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Mapping(target = "subCategory", expression = "java(subCategoryMapper.map(document.getDocumentSubCategory()))")
// TODO delete 'documentSubCategory' field after 29-01-2024
@Mapping(target = "documentSubCategory", expression = "java(document.getDocumentSubCategory().getOnlyOldCategories())")
public @interface MapDocumentSubCategory {
}
