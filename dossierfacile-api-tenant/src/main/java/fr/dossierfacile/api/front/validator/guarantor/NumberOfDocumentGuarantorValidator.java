package fr.dossierfacile.api.front.validator.guarantor;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.annotation.guarantor.NumberOfDocumentGuarantor;
import fr.dossierfacile.common.entity.Tenant;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class NumberOfDocumentGuarantorValidator extends TenantConstraintValidator<NumberOfDocumentGuarantor, DocumentGuarantorFormAbstract> {

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
        Tenant tenant = getTenant(documentGuarantorFormAbstract);
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
