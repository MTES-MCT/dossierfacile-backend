package fr.dossierfacile.api.front.validator.guarantor.natural_person.tax;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentTaxGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.validator.annotation.guarantor.natural_person.tax.OtherTaxCustomTextGuarantorNaturalPerson;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static fr.dossierfacile.api.front.validator.CustomTextValidation.requireCustomText;

public class OtherTaxCustomTextGuarantorNaturalPersonValidator
        implements
            ConstraintValidator<OtherTaxCustomTextGuarantorNaturalPerson, DocumentTaxGuarantorNaturalPersonForm> {

    @Override
    public boolean isValid(DocumentTaxGuarantorNaturalPersonForm documentTaxGuarantorNaturalPersonForm, ConstraintValidatorContext constraintValidatorContext) {
        if (documentTaxGuarantorNaturalPersonForm.getTypeDocumentTax() == DocumentSubCategory.OTHER_TAX && documentTaxGuarantorNaturalPersonForm.getNoDocument()
                && documentTaxGuarantorNaturalPersonForm.getCategoryStep() == null) {
            return requireCustomText(documentTaxGuarantorNaturalPersonForm.getCustomText(), constraintValidatorContext);
        }
        return true;
    }
}
