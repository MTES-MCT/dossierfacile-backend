package fr.dossierfacile.api.front.validator.guarantor.natural_person.tax;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentTaxGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.tax.NumberOfDocumentTaxGuarantorNaturalPerson;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentTaxGuarantorNaturalPersonValidator extends TenantConstraintValidator<NumberOfDocumentTaxGuarantorNaturalPerson, DocumentTaxGuarantorNaturalPersonForm> {

    private static final String DOCUMENTS = "documents";
    private static final String RESPONSE = "number of document must be less than 15";

    private final FileRepository fileRepository;

    @Override
    public boolean isValid(DocumentTaxGuarantorNaturalPersonForm documentTaxGuarantorNaturalPersonForm, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = getTenant(documentTaxGuarantorNaturalPersonForm);
        long countOld = fileRepository.countFileByDocumentCategoryGuarantorIdTypeGuarantorTenant(
                DocumentCategory.TAX,
                documentTaxGuarantorNaturalPersonForm.getGuarantorId(),
                TypeGuarantor.NATURAL_PERSON,
                tenant
        );
        long countNew = documentTaxGuarantorNaturalPersonForm.getDocuments().stream().filter(f -> !f.isEmpty()).count();

        boolean isValid;
        if (documentTaxGuarantorNaturalPersonForm.getTypeDocumentTax() == DocumentSubCategory.MY_NAME) {
            isValid = 1 <= countNew + countOld && countNew + countOld <= 5;
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate(RESPONSE)
                        .addPropertyNode(DOCUMENTS).addConstraintViolation();
            }
        } else if (documentTaxGuarantorNaturalPersonForm.getTypeDocumentTax() == DocumentSubCategory.OTHER_TAX
                && !documentTaxGuarantorNaturalPersonForm.getNoDocument()) {
            isValid = 1 <= countNew + countOld && countNew + countOld <= 5;
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate(RESPONSE)
                        .addPropertyNode(DOCUMENTS).addConstraintViolation();
            }
        } else {
            isValid = countNew + countOld == 0;
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate("number of document must be 0")
                        .addPropertyNode(DOCUMENTS).addConstraintViolation();
            }
        }
        return isValid;
    }
}
