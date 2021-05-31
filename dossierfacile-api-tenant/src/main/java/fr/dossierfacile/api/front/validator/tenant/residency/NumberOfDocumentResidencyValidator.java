package fr.dossierfacile.api.front.validator.tenant.residency;

import fr.dossierfacile.api.front.register.form.tenant.DocumentResidencyForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.tenant.residency.NumberOfDocumentResidency;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentResidencyValidator implements ConstraintValidator<NumberOfDocumentResidency, DocumentResidencyForm> {
    private final FileRepository fileRepository;
    private final AuthenticationFacade authenticationFacade;

    @Override
    public void initialize(NumberOfDocumentResidency constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentResidencyForm documentResidencyForm, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        long countOld = fileRepository.countFileByDocumentCategoryTenant(DocumentCategory.RESIDENCY, tenant);
        long countNew = documentResidencyForm.getDocuments()
                .stream()
                .filter(f -> !f.isEmpty())
                .count();
        return 1 <= countNew + countOld && countNew + countOld <= 15;
    }
}
