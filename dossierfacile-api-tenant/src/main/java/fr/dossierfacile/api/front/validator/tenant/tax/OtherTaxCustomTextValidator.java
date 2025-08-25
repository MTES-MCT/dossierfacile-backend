package fr.dossierfacile.api.front.validator.tenant.tax;

import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.validator.annotation.tenant.tax.OtherTaxCustomText;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OtherTaxCustomTextValidator implements ConstraintValidator<OtherTaxCustomText, DocumentTaxForm> {
    @Override
    public void initialize(OtherTaxCustomText constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentTaxForm documentTaxForm, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = true;
        if (documentTaxForm.getTypeDocumentTax() == DocumentSubCategory.OTHER_TAX && documentTaxForm.getNoDocument() && documentTaxForm.getCategoryStep() == null) {
            isValid = documentTaxForm.getCustomText() != null && !documentTaxForm.getCustomText().isBlank();
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
