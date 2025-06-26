package fr.dossierfacile.api.front.validator.tenant.tax;

import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.annotation.tenant.tax.NumberOfDocumentTax;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentTaxValidator extends TenantConstraintValidator<NumberOfDocumentTax, DocumentTaxForm> {

    private static final String DOCUMENTS = "documents";

    // Test visibility
    public static final String TOO_MANY_DOCUMENTS_RESPONSE = "number of document must be less than 5";
    public static final String MISSING_DOCUMENT_RESPONSE = "number of document must be at least 1";
    public static final String NO_DOCUMENT_RESPONSE = "number of document must be 0";

    private final FileRepository fileRepository;

    @Override
    public boolean isValid(DocumentTaxForm documentTaxForm, ConstraintValidatorContext constraintValidatorContext) {

        Tenant tenant = getTenant(documentTaxForm);
        long countOld = fileRepository.countFileByDocumentCategoryTenant(DocumentCategory.TAX, tenant);
        long countNew = documentTaxForm.getDocuments().stream().filter(f -> !f.isEmpty()).count();

        boolean isValid;
        if (documentTaxForm.getTypeDocumentTax() == DocumentSubCategory.MY_NAME) {
            isValid = countNew + countOld >= 1 && countNew + countOld <= 5;
            if (!isValid) {
                setInvalidMessage(constraintValidatorContext, countNew + countOld == 0);
            }
        } else if (documentTaxForm.getTypeDocumentTax() == DocumentSubCategory.OTHER_TAX && !documentTaxForm.getNoDocument()) {
            isValid = countNew + countOld >= 1 && countNew + countOld <= 5;
            if (!isValid) {
                setInvalidMessage(constraintValidatorContext, countNew + countOld == 0);
            }
        } else {
            isValid = countNew + countOld == 0;
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate(NO_DOCUMENT_RESPONSE)
                        .addPropertyNode(DOCUMENTS).addConstraintViolation();
            }
        }
        return isValid;
    }

    private void setInvalidMessage(ConstraintValidatorContext context, boolean isEmpty) {
        context.disableDefaultConstraintViolation();
        var message = isEmpty ? MISSING_DOCUMENT_RESPONSE : TOO_MANY_DOCUMENTS_RESPONSE;
        context
                .buildConstraintViolationWithTemplate(message)
                .addPropertyNode(DOCUMENTS).addConstraintViolation();
    }
}
