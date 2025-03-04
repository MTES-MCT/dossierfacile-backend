package fr.dossierfacile.api.front.validator.guarantor.natural_person.identification;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentIdentificationGuarantorNaturalPersonFileForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.annotation.guarantor.natural_person.identification.NumberOfDocumentIdentificationGuarantorNaturalPersonFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentIdentificationGuarantorNaturalPersonFileValidator extends TenantConstraintValidator<NumberOfDocumentIdentificationGuarantorNaturalPersonFile, DocumentIdentificationGuarantorNaturalPersonFileForm> {

    private final FileRepository fileRepository;

    @Override
    public boolean isValid(DocumentIdentificationGuarantorNaturalPersonFileForm documentIdentificationGuarantorNaturalPersonFileForm, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = getTenant(documentIdentificationGuarantorNaturalPersonFileForm);
        long countOld = fileRepository.countFileByDocumentCategoryGuarantorIdTypeGuarantorTenant(
                DocumentCategory.IDENTIFICATION,
                documentIdentificationGuarantorNaturalPersonFileForm.getGuarantorId(),
                TypeGuarantor.NATURAL_PERSON,
                tenant
        );
        long countNew = documentIdentificationGuarantorNaturalPersonFileForm.getDocuments()
                .stream()
                .filter(f -> !f.isEmpty())
                .count();
        return 1 <= countNew + countOld && countNew + countOld <= 5;
    }
}
