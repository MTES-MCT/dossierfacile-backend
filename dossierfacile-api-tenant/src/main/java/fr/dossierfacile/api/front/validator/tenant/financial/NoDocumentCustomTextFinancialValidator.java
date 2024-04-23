package fr.dossierfacile.api.front.validator.tenant.financial;

import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
import fr.dossierfacile.api.front.validator.annotation.tenant.financial.NoDocumentCustomTextFinancial;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

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
        boolean noDocument = documentFinancialForm.getNoDocument();
        boolean isValid = true;
        if (noDocument) {
            isValid = documentFinancialForm.getCustomText() != null && !documentFinancialForm.getCustomText().isBlank();
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate("{jakarta.validation.constraints.NotEmpty.message}")
                        .addPropertyNode("customText").addConstraintViolation();
            }
        }
        return isValid;
    }
}
