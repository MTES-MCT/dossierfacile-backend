package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;

@RequiredArgsConstructor
public abstract class NumberOfDocumentTaxValidator<A extends Annotation, F extends FormWithTenantId> extends TenantConstraintValidator<A, F> {

    private static final String DOCUMENTS = "documents";

    // Test visibility
    public static final String TOO_MANY_DOCUMENTS_RESPONSE = "number of document must be less than 5";
    public static final String MISSING_DOCUMENT_RESPONSE = "number of document must be at least 1";
    public static final String NO_DOCUMENT_RESPONSE = "number of document must be 0";

    protected final FileRepository fileRepository;

    @Override
    public boolean isValid(F documentTaxForm, ConstraintValidatorContext constraintValidatorContext) {
        long countOld = getOldCount(documentTaxForm);
        long countNew = getNewCount(documentTaxForm);

        boolean isValid;
        if ((getTypeDocumentTax(documentTaxForm) == DocumentSubCategory.MY_NAME) || (getTypeDocumentTax(documentTaxForm) == DocumentSubCategory.OTHER_TAX && !getNoDocument(documentTaxForm))) {
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

    protected abstract long getOldCount(F documentTaxForm);

    protected abstract long getNewCount(F documentTaxForm);

    protected abstract DocumentSubCategory getTypeDocumentTax(F documentTaxForm);

    protected abstract boolean getNoDocument(F documentTaxForm);

}
