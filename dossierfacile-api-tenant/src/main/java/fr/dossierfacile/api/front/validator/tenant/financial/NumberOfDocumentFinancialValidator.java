package fr.dossierfacile.api.front.validator.tenant.financial;

import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.tenant.financial.NumberOfDocumentFinancial;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentFinancialValidator implements ConstraintValidator<NumberOfDocumentFinancial, DocumentFinancialForm> {
    private final FileRepository fileRepository;
    private final AuthenticationFacade authenticationFacade;

    @Override
    public void initialize(NumberOfDocumentFinancial constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentFinancialForm documentFinancialForm, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = authenticationFacade.getTenant(documentFinancialForm.getTenantId());
        long sizeOldDoc = 0;
        long countOld = fileRepository.countFileByDocumentCategoryTenantDocumentId(DocumentCategory.FINANCIAL, tenant, documentFinancialForm.getId());
        long countNew = documentFinancialForm.getDocuments().stream().filter(f -> !f.isEmpty()).count();

        if (countOld > 0) {
            sizeOldDoc = fileRepository.sumSizeOfAllFilesForDocumentId(DocumentCategory.FINANCIAL, tenant, documentFinancialForm.getId());
        }
        long sizeNewDoc = documentFinancialForm.getDocuments().stream().filter(o -> o.getSize() >= 0).mapToLong(MultipartFile::getSize).sum();

        if (documentFinancialForm.getNoDocument() == null) {
            return true;
        }
        boolean noDocument = documentFinancialForm.getNoDocument();
        boolean isValid;
        if (noDocument) {
            isValid = countNew + countOld == 0;
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate("{javax.validation.constraints.Null.message}")
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
