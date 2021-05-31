package fr.dossierfacile.api.front.validator.tenant.type_guarantor;

import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.tenant.type_guarantor.MaxGuarantor;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MaxGuarantorValidator implements ConstraintValidator<MaxGuarantor, TypeGuarantor> {

    private final AuthenticationFacade authenticationFacade;

    @Override
    public void initialize(MaxGuarantor constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(TypeGuarantor typeGuarantor, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        List<Guarantor> guarantors = tenant.getGuarantors();
        int cantGuarantor = guarantors.size();
        if (typeGuarantor == TypeGuarantor.LEGAL_PERSON && cantGuarantor == 0) {
            return true;
        }
        if (typeGuarantor == TypeGuarantor.ORGANISM && cantGuarantor == 0) {
            return true;
        }
        return typeGuarantor == TypeGuarantor.NATURAL_PERSON && cantGuarantor <= 1 && isAllGuarantorLegalPerson(guarantors);
    }

    private boolean isAllGuarantorLegalPerson(List<Guarantor> guarantors) {
        return guarantors.stream().noneMatch(g -> g.getTypeGuarantor() == TypeGuarantor.ORGANISM || g.getTypeGuarantor() == TypeGuarantor.LEGAL_PERSON);
    }
}
