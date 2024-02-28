package fr.dossierfacile.api.front.validator.guarantor;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.ExistGuarantor;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExistGuarantorValidator extends TenantConstraintValidator<ExistGuarantor, DocumentGuarantorFormAbstract> {

    private final GuarantorRepository guarantorRepository;

    @Override
    public boolean isValid(DocumentGuarantorFormAbstract documentGuarantorFormAbstract, ConstraintValidatorContext constraintValidatorContext) {
        var guarantorId = documentGuarantorFormAbstract.getGuarantorId();
        var typeGuarantor = documentGuarantorFormAbstract.getTypeGuarantor();
        if (guarantorId == null) {
            return true;
        }
        var tenant = getTenant(documentGuarantorFormAbstract);
        return guarantorRepository.existsByIdAndTenantAndTypeGuarantor(guarantorId, tenant, typeGuarantor);
    }
}
