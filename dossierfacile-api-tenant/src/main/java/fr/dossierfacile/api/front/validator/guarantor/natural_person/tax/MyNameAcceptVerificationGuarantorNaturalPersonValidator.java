package fr.dossierfacile.api.front.validator.guarantor.natural_person.tax;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentTaxGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.tax.MyNameAcceptVerificationGuarantorNaturalPerson;
import fr.dossierfacile.common.enums.DocumentSubCategory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class MyNameAcceptVerificationGuarantorNaturalPersonValidator implements ConstraintValidator<MyNameAcceptVerificationGuarantorNaturalPerson, DocumentTaxGuarantorNaturalPersonForm> {
    @Override
    public void initialize(MyNameAcceptVerificationGuarantorNaturalPerson constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentTaxGuarantorNaturalPersonForm documentTaxGuarantorNaturalPersonForm, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = true;
        if (documentTaxGuarantorNaturalPersonForm.getTypeDocumentTax() == DocumentSubCategory.MY_NAME) {
            isValid = documentTaxGuarantorNaturalPersonForm.getAcceptVerification() != null && documentTaxGuarantorNaturalPersonForm.getAcceptVerification();
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
