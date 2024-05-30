package fr.dossierfacile.api.front.validator.guarantor.natural_person.tax;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentTaxGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.validator.annotation.guarantor.natural_person.tax.OtherTaxCustomTextGuarantorNaturalPerson;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OtherTaxCustomTextGuarantorNaturalPersonValidator implements ConstraintValidator<OtherTaxCustomTextGuarantorNaturalPerson, DocumentTaxGuarantorNaturalPersonForm> {

    @Override
    public boolean isValid(DocumentTaxGuarantorNaturalPersonForm documentTaxGuarantorNaturalPersonForm, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = true;
        if (documentTaxGuarantorNaturalPersonForm.getTypeDocumentTax() == DocumentSubCategory.OTHER_TAX && documentTaxGuarantorNaturalPersonForm.getNoDocument()) {
            isValid = documentTaxGuarantorNaturalPersonForm.getCustomText() != null && !documentTaxGuarantorNaturalPersonForm.getCustomText().isBlank();
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
