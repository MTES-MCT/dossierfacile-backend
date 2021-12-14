package fr.dossierfacile.api.front.validator.guarantor.natural_person.professional;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentProfessionalGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.professional.NumberOfDocumentProfessionalGuarantorNaturalPerson;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentProfessionalGuarantorNaturalPersonValidator implements ConstraintValidator<NumberOfDocumentProfessionalGuarantorNaturalPerson, DocumentProfessionalGuarantorNaturalPersonForm> {

    private final AuthenticationFacade authenticationFacade;
    private final FileRepository fileRepository;

    @Override
    public void initialize(NumberOfDocumentProfessionalGuarantorNaturalPerson constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentProfessionalGuarantorNaturalPersonForm documentProfessionalGuarantorNaturalPersonForm, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = authenticationFacade.getTenant(documentProfessionalGuarantorNaturalPersonForm.getTenantId());
        long sizeOldDoc = 0;
        long countOld = fileRepository.countFileByDocumentCategoryGuarantorIdTypeGuarantorTenant(
                DocumentCategory.PROFESSIONAL,
                documentProfessionalGuarantorNaturalPersonForm.getGuarantorId(),
                TypeGuarantor.NATURAL_PERSON,
                tenant
        );
        long countNew = documentProfessionalGuarantorNaturalPersonForm.getDocuments()
                .stream()
                .filter(f -> !f.isEmpty())
                .count();
        if(countOld > 0){
            sizeOldDoc = fileRepository.sumSizeOfAllFilesInDocumentForGuarantorTenantId(
                    DocumentCategory.PROFESSIONAL,
                    documentProfessionalGuarantorNaturalPersonForm.getGuarantorId(),
                    TypeGuarantor.NATURAL_PERSON,
                    tenant
            );
        }
        long sizeNewDoc = documentProfessionalGuarantorNaturalPersonForm.getDocuments().stream().filter(o -> o.getSize() >= 0).mapToLong(MultipartFile::getSize).sum();

        return 1 <= countNew + countOld && countNew + countOld <= 20 && sizeNewDoc + sizeOldDoc <= 52428800;
    }
}
