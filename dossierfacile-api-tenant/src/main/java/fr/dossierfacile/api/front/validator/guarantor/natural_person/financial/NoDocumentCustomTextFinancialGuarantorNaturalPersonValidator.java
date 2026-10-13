package fr.dossierfacile.api.front.validator.guarantor.natural_person.financial;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentFinancialGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.validator.annotation.guarantor.natural_person.financial.NoDocumentCustomTextFinancialGuarantorNaturalPerson;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static fr.dossierfacile.api.front.validator.CustomTextValidation.requireCustomText;

public class NoDocumentCustomTextFinancialGuarantorNaturalPersonValidator
        implements
            ConstraintValidator<NoDocumentCustomTextFinancialGuarantorNaturalPerson, DocumentFinancialGuarantorNaturalPersonForm> {
    @Override
    public void initialize(NoDocumentCustomTextFinancialGuarantorNaturalPerson constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentFinancialGuarantorNaturalPersonForm documentFinancialGuarantorNaturalPersonForm,
            ConstraintValidatorContext constraintValidatorContext) {
        if (documentFinancialGuarantorNaturalPersonForm.getNoDocument() == null) {
            return true;
        }
        if (documentFinancialGuarantorNaturalPersonForm.getNoDocument()) {
            return requireCustomText(documentFinancialGuarantorNaturalPersonForm.getCustomText(), constraintValidatorContext);
        }
        return true;
    }
}
