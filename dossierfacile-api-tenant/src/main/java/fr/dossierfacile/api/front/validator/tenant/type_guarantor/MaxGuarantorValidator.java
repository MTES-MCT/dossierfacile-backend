package fr.dossierfacile.api.front.validator.tenant.type_guarantor;

import fr.dossierfacile.api.front.register.form.tenant.GuarantorTypeForm;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.anotation.tenant.type_guarantor.MaxGuarantor;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MaxGuarantorValidator extends TenantConstraintValidator<MaxGuarantor, GuarantorTypeForm> {

    @Override
    public boolean isValid(GuarantorTypeForm guarantorTypeForm, ConstraintValidatorContext constraintValidatorContext) {
        var tenant = getTenant(guarantorTypeForm);
        if (tenant == null) {
            return true;
        }
        var typeGuarantor = guarantorTypeForm.getTypeGuarantor();
        List<Guarantor> guarantors = tenant.getGuarantors();
        int cantGuarantor = guarantors.size();
        if (typeGuarantor == TypeGuarantor.LEGAL_PERSON && cantGuarantor == 0) {
            return true;
        }
        if (typeGuarantor == TypeGuarantor.ORGANISM && cantGuarantor == 0) {
            return true;
        }
        var isValid = typeGuarantor == TypeGuarantor.NATURAL_PERSON && cantGuarantor <= 1 && isAllGuarantorLegalPerson(guarantors);
        if (!isValid) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate(constraintValidatorContext.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("typeGuarantor").addConstraintViolation();
        }
        return isValid;
    }

    private boolean isAllGuarantorLegalPerson(List<Guarantor> guarantors) {
        return guarantors.stream().noneMatch(g -> g.getTypeGuarantor() == TypeGuarantor.ORGANISM || g.getTypeGuarantor() == TypeGuarantor.LEGAL_PERSON);
    }
}
