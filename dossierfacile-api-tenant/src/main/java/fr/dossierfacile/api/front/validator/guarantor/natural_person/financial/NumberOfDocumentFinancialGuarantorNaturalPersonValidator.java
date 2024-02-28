package fr.dossierfacile.api.front.validator.guarantor.natural_person.financial;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentFinancialGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.financial.NumberOfDocumentFinancialGuarantorNaturalPerson;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentFinancialGuarantorNaturalPersonValidator extends TenantConstraintValidator<NumberOfDocumentFinancialGuarantorNaturalPerson, DocumentFinancialGuarantorNaturalPersonForm> {
    private final FileRepository fileRepository;

    @Override
    public boolean isValid(DocumentFinancialGuarantorNaturalPersonForm documentFinancialGuarantorNaturalPersonForm, ConstraintValidatorContext constraintValidatorContext) {
        var tenant = getTenant(documentFinancialGuarantorNaturalPersonForm);
        long sizeOldDoc = 0;
        long countOld = fileRepository.countFileByDocumentCategoryGuarantorIdTypeGuarantorTenantDocumentId(
                DocumentCategory.FINANCIAL,
                documentFinancialGuarantorNaturalPersonForm.getGuarantorId(),
                TypeGuarantor.NATURAL_PERSON,
                tenant,
                documentFinancialGuarantorNaturalPersonForm.getDocumentId()
        );
        long countNew = documentFinancialGuarantorNaturalPersonForm.getDocuments().stream().filter(f -> !f.isEmpty()).count();

        long sizeNewDoc = documentFinancialGuarantorNaturalPersonForm.getDocuments().stream().filter(o -> o.getSize() >= 0).mapToLong(MultipartFile::getSize).sum();
        if(countOld > 0){
            sizeOldDoc = fileRepository.sumSizeOfAllFilesInDocumentForGuarantorTenantId(
                    DocumentCategory.FINANCIAL,
                    documentFinancialGuarantorNaturalPersonForm.getGuarantorId(),
                    TypeGuarantor.NATURAL_PERSON,
                    tenant,
                    documentFinancialGuarantorNaturalPersonForm.getDocumentId()
            );
        }


        if (documentFinancialGuarantorNaturalPersonForm.getNoDocument() == null) {
            return true;
        }

        boolean noDocument = documentFinancialGuarantorNaturalPersonForm.getNoDocument();
        boolean isValid;
        if (noDocument) {
            isValid = countNew + countOld == 0;
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate("{jakarta.validation.constraints.Null.message}")
                        .addPropertyNode("documents").addConstraintViolation();
            }

        } else {
            isValid = 1 <= countNew + countOld && countNew + countOld <= 10 && sizeNewDoc + sizeOldDoc <= 52428800;
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate("number of document must be between 1 and 10 and not exceed 50Mb in total")
                        .addPropertyNode("documents").addConstraintViolation();
            }
        }
        return isValid;
    }
}
