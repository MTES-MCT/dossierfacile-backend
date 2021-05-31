package fr.dossierfacile.api.front.validator.guarantor;

import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.guarantor.NumberOfDocumentGuarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentGuarantorValidator implements ConstraintValidator<NumberOfDocumentGuarantor, List<MultipartFile>> {

    private final AuthenticationFacade authenticationFacade;
    private final DocumentRepository documentRepository;
    private final GuarantorRepository guarantorRepository;
    private int max;
    private int min;
    private DocumentCategory documentCategory;
    private TypeGuarantor typeGuarantor;

    @Override
    public void initialize(NumberOfDocumentGuarantor constraintAnnotation) {
        max = constraintAnnotation.max();
        min = constraintAnnotation.min();
        documentCategory = constraintAnnotation.documentCategory();
        typeGuarantor = constraintAnnotation.typeGuarantor();
    }

    @Override
    public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        AtomicLong countOld = new AtomicLong();
        guarantorRepository.findByTenantAndTypeGuarantor(tenant, typeGuarantor)
                .ifPresent(g -> countOld.set(documentRepository.countByDocumentCategoryAndGuarantor(documentCategory, g)));
        long countNew = files
                .stream()
                .filter(f -> !f.isEmpty())
                .count();
        return min <= countNew + countOld.get() && countNew + countOld.get() <= max;
    }
}
