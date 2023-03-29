package fr.dossierfacile.api.front.validator.tenant.profesional;

import fr.dossierfacile.api.front.register.form.tenant.DocumentProfessionalForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.anotation.tenant.profesional.NumberOfDocumentProfesional;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentProfesionalValidator extends TenantConstraintValidator<NumberOfDocumentProfesional, DocumentProfessionalForm> {

    private final FileRepository fileRepository;
    private int max;
    private int min;

    @Override
    public void initialize(NumberOfDocumentProfesional constraintAnnotation) {
        max = constraintAnnotation.max();
        min = constraintAnnotation.min();
    }

    @Override
    public boolean isValid(DocumentProfessionalForm documentProfessionalForm, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = getTenant(documentProfessionalForm);
        var files = documentProfessionalForm.getDocuments();
        long sizeOldDoc = 0;
        long countOld = fileRepository.countFileByDocumentCategoryTenant(DocumentCategory.PROFESSIONAL, tenant);
        long countNew = files
                .stream()
                .filter(f -> !f.isEmpty())
                .count();

        if (countOld > 0) {
            sizeOldDoc = fileRepository.sumSizeOfAllFilesForDocument(DocumentCategory.PROFESSIONAL, tenant);
        }
        long sizeNewDoc = files.stream().filter(o -> o.getSize() >= 0).mapToLong(MultipartFile::getSize).sum();

        var isValid = min <= countNew + countOld && countNew + countOld <= max && sizeNewDoc + sizeOldDoc <= 52428800;
        if (!isValid) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate(constraintValidatorContext.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("documents").addConstraintViolation();
        }
        return isValid;
    }
}
