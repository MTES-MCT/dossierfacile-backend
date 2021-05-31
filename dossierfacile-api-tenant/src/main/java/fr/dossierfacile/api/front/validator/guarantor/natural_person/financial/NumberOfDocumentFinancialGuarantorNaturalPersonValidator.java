package fr.dossierfacile.api.front.validator.guarantor.natural_person.financial;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentFinancialGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.financial.NumberOfDocumentFinancialGuarantorNaturalPerson;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentFinancialGuarantorNaturalPersonValidator implements ConstraintValidator<NumberOfDocumentFinancialGuarantorNaturalPerson, DocumentFinancialGuarantorNaturalPersonForm> {
    private final AuthenticationFacade authenticationFacade;
    private final FileRepository fileRepository;

    @Override
    public void initialize(NumberOfDocumentFinancialGuarantorNaturalPerson constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentFinancialGuarantorNaturalPersonForm documentFinancialGuarantorNaturalPersonForm, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        long countOld = fileRepository.countFileByDocumentCategoryGuarantorIdTypeGuarantorTenantDocumentId(
                DocumentCategory.FINANCIAL,
                documentFinancialGuarantorNaturalPersonForm.getGuarantorId(),
                TypeGuarantor.NATURAL_PERSON,
                tenant,
                documentFinancialGuarantorNaturalPersonForm.getDocumentId()
        );
        long countNew = documentFinancialGuarantorNaturalPersonForm.getDocuments().stream().filter(f -> !f.isEmpty()).count();

        if (documentFinancialGuarantorNaturalPersonForm.getNoDocument() == null) {
            return true;
        }

        boolean noDocument = documentFinancialGuarantorNaturalPersonForm.getNoDocument();
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
