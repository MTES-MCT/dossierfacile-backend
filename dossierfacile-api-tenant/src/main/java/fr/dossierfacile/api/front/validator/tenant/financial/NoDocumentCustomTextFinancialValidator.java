package fr.dossierfacile.api.front.validator.tenant.financial;

import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
import fr.dossierfacile.api.front.validator.anotation.tenant.financial.NoDocumentCustomTextFinancial;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
                        .buildConstraintViolationWithTemplate("{javax.validation.constraints.NotEmpty.message}")
                        .addPropertyNode("customText").addConstraintViolation();
            }
        }
        return isValid;
    }
}
