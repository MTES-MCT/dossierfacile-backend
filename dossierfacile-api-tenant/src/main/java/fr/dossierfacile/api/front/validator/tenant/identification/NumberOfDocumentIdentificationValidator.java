package fr.dossierfacile.api.front.validator.tenant.identification;

import fr.dossierfacile.api.front.register.form.tenant.DocumentIdentificationForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.anotation.tenant.identification.NumberOfDocumentIdentification;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentIdentificationValidator extends TenantConstraintValidator<NumberOfDocumentIdentification, DocumentIdentificationForm> {

    private final FileRepository fileRepository;
    private int max;
    private int min;

    @Override
    public void initialize(NumberOfDocumentIdentification constraintAnnotation) {
        max = constraintAnnotation.max();
        min = constraintAnnotation.min();
    }

    @Override
    public boolean isValid(DocumentIdentificationForm documentIdentificationForm, ConstraintValidatorContext constraintValidatorContext) {
        var tenant = getTenant(documentIdentificationForm);
        var files = documentIdentificationForm.getDocuments();
        long sizeOldDoc = 0;
        long countOld = fileRepository.countFileByDocumentCategoryTenant(DocumentCategory.IDENTIFICATION, tenant);
        long countNew = files
                .stream()
                .filter(f -> !f.isEmpty())
                .count();

        if (countOld > 0) {
            sizeOldDoc = fileRepository.sumSizeOfAllFilesForDocument(DocumentCategory.IDENTIFICATION, tenant);
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
