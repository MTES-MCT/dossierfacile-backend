package fr.dossierfacile.api.front.validator.common.financial;

import fr.dossierfacile.api.front.register.form.IDocumentFinancialForm;
import fr.dossierfacile.api.front.validator.annotation.common.financial.MonthlySumValue;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MonthlySumValueValidator implements ConstraintValidator<MonthlySumValue, IDocumentFinancialForm> {
    @Override
    public void initialize(MonthlySumValue constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    // Check monthlySum is strictly positive if not NO_INCOME financialType
    @Override
    public boolean isValid(IDocumentFinancialForm documentFinancialForm, ConstraintValidatorContext constraintValidatorContext) {
        return documentFinancialForm.getTypeDocumentFinancial() == DocumentSubCategory.NO_INCOME
                || (documentFinancialForm.getMonthlySum() != null && documentFinancialForm.getMonthlySum() > 0);
    }
}
