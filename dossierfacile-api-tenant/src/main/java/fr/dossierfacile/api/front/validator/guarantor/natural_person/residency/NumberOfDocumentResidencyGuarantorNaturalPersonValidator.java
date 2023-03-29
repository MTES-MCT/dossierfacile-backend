package fr.dossierfacile.api.front.validator.guarantor.natural_person.residency;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentResidencyGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.residency.NumberOfDocumentResidencyGuarantorNaturalPerson;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentResidencyGuarantorNaturalPersonValidator extends TenantConstraintValidator<NumberOfDocumentResidencyGuarantorNaturalPerson, DocumentResidencyGuarantorNaturalPersonForm> {

    private final FileRepository fileRepository;

    @Override
    public boolean isValid(DocumentResidencyGuarantorNaturalPersonForm documentResidencyGuarantorNaturalPersonForm, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = getTenant(documentResidencyGuarantorNaturalPersonForm);
        long sizeOldDoc = 0;
        long countOld = fileRepository.countFileByDocumentCategoryGuarantorIdTypeGuarantorTenant(
                DocumentCategory.RESIDENCY,
                documentResidencyGuarantorNaturalPersonForm.getGuarantorId(),
                TypeGuarantor.NATURAL_PERSON,
                tenant
        );
        long countNew = documentResidencyGuarantorNaturalPersonForm.getDocuments()
                .stream()
                .filter(f -> !f.isEmpty())
                .count();
        if (countOld > 0) {
            sizeOldDoc = fileRepository.sumSizeOfAllFilesInDocumentForGuarantorTenant(
                    DocumentCategory.RESIDENCY,
                    documentResidencyGuarantorNaturalPersonForm.getGuarantorId(),
                    TypeGuarantor.NATURAL_PERSON,
                    tenant
            );
        }
        long sizeNewDoc = documentResidencyGuarantorNaturalPersonForm.getDocuments().stream().filter(o -> o.getSize() >= 0).mapToLong(MultipartFile::getSize).sum();

        return 1 <= countNew + countOld && countNew + countOld <= 10 && sizeNewDoc + sizeOldDoc <= 52428800;
    }
}
