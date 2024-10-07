package fr.dossierfacile.common.mapper;

import org.mapstruct.Mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@Mapping(target = "documentCategory", expression = "java(categoriesMapper.mapCategory(document.getDocumentCategory(), userApi))")
@Mapping(target = "documentSubCategory", expression = "java(categoriesMapper.mapSubCategory(document.getDocumentSubCategory(), userApi))")
// TODO use documentSubCategory
@Deprecated
@Mapping(target = "subCategory", expression = "java(userApi == null ? categoriesMapper.mapSubCategory(document.getDocumentSubCategory(), userApi) : null)")
public @interface MapDocumentCategories {
}
