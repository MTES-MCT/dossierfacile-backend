package fr.dossierfacile.api.front.validator.guarantor.natural_person.financial;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentFinancialGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.financial.NoDocumentCustomTextFinancialGuarantorNaturalPerson;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NoDocumentCustomTextFinancialGuarantorNaturalPersonValidator implements ConstraintValidator<NoDocumentCustomTextFinancialGuarantorNaturalPerson, DocumentFinancialGuarantorNaturalPersonForm> {
    @Override
    public void initialize(NoDocumentCustomTextFinancialGuarantorNaturalPerson constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentFinancialGuarantorNaturalPersonForm documentFinancialGuarantorNaturalPersonForm, ConstraintValidatorContext constraintValidatorContext) {
        if (documentFinancialGuarantorNaturalPersonForm.getNoDocument() == null) {
            return true;
        }
        boolean noDocument = documentFinancialGuarantorNaturalPersonForm.getNoDocument();
        boolean isValid = true;
        if (noDocument) {
            isValid = documentFinancialGuarantorNaturalPersonForm.getCustomText() != null && !documentFinancialGuarantorNaturalPersonForm.getCustomText().isBlank();
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
