package fr.dossierfacile.api.front.validator.tenant.financial;

import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
import fr.dossierfacile.api.front.validator.annotation.tenant.financial.NoDocumentCustomTextFinancial;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static fr.dossierfacile.api.front.validator.CustomTextValidation.requireCustomText;

public class NoDocumentCustomTextFinancialValidator implements ConstraintValidator<NoDocumentCustomTextFinancial, DocumentFinancialForm> {
    @Override
    public void initialize(NoDocumentCustomTextFinancial constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentFinancialForm documentFinancialForm, ConstraintValidatorContext constraintValidatorContext) {
        if (documentFinancialForm.getNoDocument() == null) {
            return true;
        }
        if (documentFinancialForm.getNoDocument()) {
            return requireCustomText(documentFinancialForm.getCustomText(), constraintValidatorContext);
        }
        return true;
    }
}
