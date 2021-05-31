package fr.dossierfacile.api.front.validator.tenant.financial;

import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.tenant.financial.NumberOfDocumentFinancial;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        long countOld = fileRepository.countFileByDocumentCategoryTenantDocumentId(DocumentCategory.FINANCIAL, tenant, documentFinancialForm.getId());
        long countNew = documentFinancialForm.getDocuments().stream().filter(f -> !f.isEmpty()).count();

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
            isValid = 1 <= countNew + countOld && countNew + countOld <= 15;
            if (!isValid) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate("number of document must be between 1 and 15")
                        .addPropertyNode("documents").addConstraintViolation();
            }
        }
        return isValid;
    }
}
