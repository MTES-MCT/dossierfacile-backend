package fr.dossierfacile.api.front.validator.guarantor.natural_person.identification;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentIdentificationGuarantorNaturalPersonFileForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.identification.NumberOfDocumentIdentificationGuarantorNaturalPersonFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
