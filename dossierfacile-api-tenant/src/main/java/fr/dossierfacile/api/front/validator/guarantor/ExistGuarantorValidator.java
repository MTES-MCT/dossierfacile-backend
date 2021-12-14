package fr.dossierfacile.api.front.validator.guarantor;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.ExistGuarantor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class ExistGuarantorValidator implements ConstraintValidator<ExistGuarantor, DocumentGuarantorFormAbstract> {

    private final AuthenticationFacade authenticationFacade;
    private final GuarantorRepository guarantorRepository;

    @Override
    public void initialize(ExistGuarantor constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(DocumentGuarantorFormAbstract documentGuarantorFormAbstract, ConstraintValidatorContext constraintValidatorContext) {
        var guarantorId = documentGuarantorFormAbstract.getGuarantorId();
        var typeGuarantor = documentGuarantorFormAbstract.getTypeGuarantor();
        if (guarantorId == null) {
            return true;
        }
        var tenant = authenticationFacade.getTenant(documentGuarantorFormAbstract.getTenantId());
        return guarantorRepository.existsByIdAndTenantAndTypeGuarantor(guarantorId, tenant, typeGuarantor);
    }
}
