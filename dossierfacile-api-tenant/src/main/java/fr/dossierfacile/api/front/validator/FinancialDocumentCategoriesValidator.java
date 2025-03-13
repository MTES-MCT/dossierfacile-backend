package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.register.form.IDocumentFinancialForm;
import fr.dossierfacile.api.front.validator.annotation.FinancialDocument;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

import static fr.dossierfacile.common.enums.DocumentCategoryStep.*;

public class FinancialDocumentCategoriesValidator implements ConstraintValidator<FinancialDocument, IDocumentFinancialForm> {

    private static final String VALIDATION_PROPERTY_NODE_NAME = "categoryStep";

    @Override
    public void initialize(FinancialDocument constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(IDocumentFinancialForm documentFinancialForm, ConstraintValidatorContext constraintValidatorContext) {
        switch (documentFinancialForm.getTypeDocumentFinancial()) {
            case SALARY -> {
                List<DocumentCategoryStep> listOfAllowedSubTypes = List.of(
                        SALARY_EMPLOYED_LESS_3_MONTHS,
                        SALARY_EMPLOYED_MORE_3_MONTHS,
                        SALARY_EMPLOYED_NOT_YET,
                        SALARY_FREELANCE_AUTOENTREPRENEUR,
                        SALARY_FREELANCE_OTHER,
                        SALARY_INTERMITTENT,
                        SALARY_ARTIST_AUTHOR,
                        SALARY_UNKNOWN
                );
                return verifyMandatoryStep(
                        DocumentSubCategory.SALARY,
                        documentFinancialForm.getCategoryStep(),
                        listOfAllowedSubTypes,
                        constraintValidatorContext
                );
            }
            case SOCIAL_SERVICE -> {
                List<DocumentCategoryStep> listOfAllowedSubTypes = List.of(
                        SOCIAL_SERVICE_CAF_LESS_3_MONTHS,
                        SOCIAL_SERVICE_CAF_MORE_3_MONTHS,
                        SOCIAL_SERVICE_FRANCE_TRAVAIL_LESS_3_MONTHS,
                        SOCIAL_SERVICE_FRANCE_TRAVAIL_MORE_3_MONTHS,
                        SOCIAL_SERVICE_FRANCE_TRAVAIL_NOT_YET,
                        SOCIAL_SERVICE_APL_LESS_3_MONTHS,
                        SOCIAL_SERVICE_APL_MORE_3_MONTHS,
                        SOCIAL_SERVICE_APL_NOT_YET,
                        SOCIAL_SERVICE_AAH_LESS_3_MONTHS,
                        SOCIAL_SERVICE_AAH_MORE_3_MONTHS,
                        SOCIAL_SERVICE_AAH_NOT_YET,
                        SOCIAL_SERVICE_OTHER
                );
                return verifyMandatoryStep(
                        DocumentSubCategory.SOCIAL_SERVICE,
                        documentFinancialForm.getCategoryStep(),
                        listOfAllowedSubTypes,
                        constraintValidatorContext
                );
            }
            case RENT -> {
                List<DocumentCategoryStep> listOfAllowedSubTypes = List.of(
                        RENT_RENTAL_RECEIPT,
                        RENT_RENTAL_NO_RECEIPT,
                        RENT_ANNUITY_LIFE,
                        RENT_OTHER
                );
                return verifyMandatoryStep(
                        DocumentSubCategory.RENT,
                        documentFinancialForm.getCategoryStep(),
                        listOfAllowedSubTypes,
                        constraintValidatorContext
                );
            }
            case PENSION -> {
                List<DocumentCategoryStep> listOfAllowedSubTypes = List.of(
                        PENSION_STATEMENT,
                        PENSION_NO_STATEMENT,
                        PENSION_DISABILITY_LESS_3_MONTHS,
                        PENSION_DISABILITY_MORE_3_MONTHS,
                        PENSION_DISABILITY_NOT_YET,
                        PENSION_ALIMONY,
                        PENSION_UNKNOWN
                );
                return verifyMandatoryStep(
                        DocumentSubCategory.PENSION,
                        documentFinancialForm.getCategoryStep(),
                        listOfAllowedSubTypes,
                        constraintValidatorContext
                );
            }
            case SCHOLARSHIP, NO_INCOME -> {
                if (documentFinancialForm.getCategoryStep() != null) {
                    constraintValidatorContext.disableDefaultConstraintViolation();
                    constraintValidatorContext.buildConstraintViolationWithTemplate("For document sub category " + documentFinancialForm.getTypeDocumentFinancial().name() + " category step has to be null")
                            .addPropertyNode(VALIDATION_PROPERTY_NODE_NAME)
                            .addConstraintViolation();
                    return false;
                }
                return true;
            }
            default -> {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate(documentFinancialForm.getTypeDocumentFinancial().name() + " is not a financial sub category")
                        .addPropertyNode(VALIDATION_PROPERTY_NODE_NAME)
                        .addConstraintViolation();
                return false;
            }
        }
    }

    private boolean verifyMandatoryStep(
            DocumentSubCategory subCategory,
            DocumentCategoryStep categoryStep,
            List<DocumentCategoryStep> availableStep,
            ConstraintValidatorContext constraintValidatorContext) {
        if (categoryStep == null) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate("For document sub category " + subCategory.name() + " category step can not be null")
                    .addPropertyNode(VALIDATION_PROPERTY_NODE_NAME)
                    .addConstraintViolation();
            return false;
        }
        if (!availableStep.contains(categoryStep)) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(categoryStep.name() + " is not valid for document sub category " + subCategory.name())
                    .addPropertyNode(VALIDATION_PROPERTY_NODE_NAME)
                    .addConstraintViolation();
            return false;
        }
        return true;

    }
}
