package fr.dossierfacile.api.front.validator.guarantor;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.guarantor.NumberOfDocumentGuarantor;
import fr.dossierfacile.common.entity.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentGuarantorValidator implements ConstraintValidator<NumberOfDocumentGuarantor, DocumentGuarantorFormAbstract> {

    private final AuthenticationFacade authenticationFacade;
    private final DocumentRepository documentRepository;
    private final GuarantorRepository guarantorRepository;
    private int max;
    private int min;

    @Override
    public void initialize(NumberOfDocumentGuarantor constraintAnnotation) {
        max = constraintAnnotation.max();
        min = constraintAnnotation.min();
    }

    @Override
    public boolean isValid(DocumentGuarantorFormAbstract documentGuarantorFormAbstract, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = authenticationFacade.getTenant(documentGuarantorFormAbstract.getTenantId());
        AtomicLong countOld = new AtomicLong();
        guarantorRepository.findFirstByTenantAndTypeGuarantor(tenant, documentGuarantorFormAbstract.getTypeGuarantor())
                .ifPresent(g -> countOld.set(documentRepository.countByDocumentCategoryAndGuarantor(documentGuarantorFormAbstract.getDocumentCategory(), g)));
        long countNew = documentGuarantorFormAbstract.getDocuments()
                .stream()
                .filter(f -> !f.isEmpty())
                .count();
        return min <= countNew + countOld.get() && countNew + countOld.get() <= max;
    }
}
