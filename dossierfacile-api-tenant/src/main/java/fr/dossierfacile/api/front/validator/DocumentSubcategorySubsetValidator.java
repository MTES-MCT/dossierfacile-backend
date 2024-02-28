package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.validator.anotation.DocumentSubcategorySubset;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class DocumentSubcategorySubsetValidator implements ConstraintValidator<DocumentSubcategorySubset, DocumentSubCategory> {
    private DocumentSubCategory[] anyOf;

    @Override
    public void initialize(DocumentSubcategorySubset constraintAnnotation) {
        this.anyOf = constraintAnnotation.anyOf();
    }

    @Override
    public boolean isValid(DocumentSubCategory documentSubCategory, ConstraintValidatorContext constraintValidatorContext) {
        return documentSubCategory == null || Arrays.asList(anyOf).contains(documentSubCategory);
    }
}
