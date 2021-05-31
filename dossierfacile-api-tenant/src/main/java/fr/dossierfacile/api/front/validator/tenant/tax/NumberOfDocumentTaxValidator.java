package fr.dossierfacile.api.front.validator.tenant.tax;

import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.tenant.tax.NumberOfDocumentTax;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentTaxValidator implements ConstraintValidator<NumberOfDocumentTax, DocumentTaxForm> {

    private static final String DOCUMENTS = "documents";
    private static final String RESPONSE = "number of document must be less than 15";

    private final AuthenticationFacade authenticationFacade;
    private final FileRepository fileRepository;

    @Override
    public void initialize(NumberOfDocumentTax constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentTaxForm documentTaxForm, ConstraintValidatorContext constraintValidatorContext) {

        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        long countOld = fileRepository.countFileByDocumentCategoryTenant(DocumentCategory.TAX, tenant);
        long countNew = documentTaxForm.getDocuments().stream().filter(f -> !f.isEmpty()).count();

        boolean isValid;
        if (documentTaxForm.getTypeDocumentTax() == DocumentSubCategory.MY_NAME) {
            isValid = countNew + countOld >= 1 && countNew + countOld <= 15;
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate(RESPONSE)
                        .addPropertyNode(DOCUMENTS).addConstraintViolation();
            }
        } else if (documentTaxForm.getTypeDocumentTax() == DocumentSubCategory.OTHER_TAX && !documentTaxForm.getNoDocument()) {
            isValid = countNew + countOld >= 1 && countNew + countOld <= 15;
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate(RESPONSE)
                        .addPropertyNode(DOCUMENTS).addConstraintViolation();
            }
        } else {
            isValid = countNew + countOld == 0;
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate("number of document must be 0")
                        .addPropertyNode(DOCUMENTS).addConstraintViolation();
            }
        }
        return isValid;
    }
}
