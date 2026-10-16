package fr.dossierfacile.api.front.validator.tenant.tax;

import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.validator.annotation.tenant.tax.OtherTaxCustomText;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static fr.dossierfacile.api.front.validator.CustomTextValidation.requireCustomText;

public class OtherTaxCustomTextValidator implements ConstraintValidator<OtherTaxCustomText, DocumentTaxForm> {
    @Override
    public void initialize(OtherTaxCustomText constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentTaxForm documentTaxForm, ConstraintValidatorContext constraintValidatorContext) {
        if (documentTaxForm.getTypeDocumentTax() == DocumentSubCategory.OTHER_TAX && documentTaxForm.getNoDocument()
                && documentTaxForm.getCategoryStep() == null) {
            return requireCustomText(documentTaxForm.getCustomText(), constraintValidatorContext);
        }
        return true;
    }
}
