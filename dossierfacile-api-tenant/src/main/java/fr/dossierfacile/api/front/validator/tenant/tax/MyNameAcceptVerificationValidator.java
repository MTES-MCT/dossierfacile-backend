package fr.dossierfacile.api.front.validator.tenant.tax;

import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.validator.anotation.tenant.tax.MyNameAcceptVerification;
import fr.dossierfacile.common.enums.DocumentSubCategory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class MyNameAcceptVerificationValidator implements ConstraintValidator<MyNameAcceptVerification, DocumentTaxForm> {
    @Override
    public void initialize(MyNameAcceptVerification constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentTaxForm documentTaxForm, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = true;
        if (documentTaxForm.getTypeDocumentTax() == DocumentSubCategory.MY_NAME) {
            isValid = documentTaxForm.getAcceptVerification() != null && documentTaxForm.getAcceptVerification();
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate("{javax.validation.constraints.AssertTrue.message}")
                        .addPropertyNode("acceptVerification").addConstraintViolation();
            }
        }
        return isValid;
    }
}
