package fr.dossierfacile.api.front.validator.guarantor.natural_person.identification;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentIdentificationGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.identification.NumberOfDocumentIdentificationGuarantorNaturalPerson;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentIdentificationGuarantorNaturalPersonValidator extends TenantConstraintValidator<NumberOfDocumentIdentificationGuarantorNaturalPerson, DocumentIdentificationGuarantorNaturalPersonForm> {

    private final FileRepository fileRepository;

    @Override
    public boolean isValid(DocumentIdentificationGuarantorNaturalPersonForm documentIdentificationGuarantorNaturalPersonForm, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = getTenant(documentIdentificationGuarantorNaturalPersonForm);
        long countOld = fileRepository.countFileByDocumentCategoryGuarantorIdTypeGuarantorTenant(
                DocumentCategory.IDENTIFICATION,
                documentIdentificationGuarantorNaturalPersonForm.getGuarantorId(),
                TypeGuarantor.NATURAL_PERSON,
                tenant
        );
        long countNew = documentIdentificationGuarantorNaturalPersonForm.getDocuments()
                .stream()
                .filter(f -> !f.isEmpty())
                .count();
        return 1 <= countNew + countOld && countNew + countOld <= 5;
    }
}
