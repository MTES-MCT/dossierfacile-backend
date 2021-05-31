package fr.dossierfacile.api.front.validator.guarantor;

import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.ExistGuarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class ExistGuarantorValidator implements ConstraintValidator<ExistGuarantor, Long> {

    private final AuthenticationFacade authenticationFacade;
    private final GuarantorRepository guarantorRepository;
    private TypeGuarantor typeGuarantor;

    @Override
    public void initialize(ExistGuarantor constraintAnnotation) {
        typeGuarantor = constraintAnnotation.typeGuarantor();
    }

    @Override
    public boolean isValid(Long guarantorId, ConstraintValidatorContext constraintValidatorContext) {
        if (guarantorId == null) {
            return true;
        }
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        return guarantorRepository.existsByIdAndTenantAndTypeGuarantor(guarantorId, tenant, typeGuarantor);
    }
}
