package fr.dossierfacile.document.analysis.rule.validator.document_ia.mapper;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DocumentIAModel {

    DocumentCategory documentCategory();

    DocumentSubCategory documentSubCategory() default DocumentSubCategory.UNDEFINED;

    DocumentCategoryStep documentCategoryStep() default DocumentCategoryStep.UNDEFINED;
}
