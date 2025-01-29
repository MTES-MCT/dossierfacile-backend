package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.validator.annotation.DocumentCategoryStepSubset;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class DocumentCategoryStepSubsetValidator implements ConstraintValidator<DocumentCategoryStepSubset, DocumentCategoryStep> {
    private DocumentCategoryStep[] anyOf;

    @Override
    public void initialize(DocumentCategoryStepSubset constraintAnnotation) {
        this.anyOf = constraintAnnotation.anyOf();
    }

    @Override
    public boolean isValid(DocumentCategoryStep documentCategoryStep, ConstraintValidatorContext constraintValidatorContext) {
        return documentCategoryStep == null || Arrays.asList(anyOf).contains(documentCategoryStep);
    }
}
