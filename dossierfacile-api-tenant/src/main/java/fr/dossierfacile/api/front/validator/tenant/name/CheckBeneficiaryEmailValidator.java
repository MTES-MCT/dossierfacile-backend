package fr.dossierfacile.api.front.validator.tenant.name;

import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.validator.annotation.tenant.name.CheckBeneficiaryEmail;
import fr.dossierfacile.common.enums.TenantOwnerType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class CheckBeneficiaryEmailValidator implements ConstraintValidator<CheckBeneficiaryEmail, NamesForm> {

    @Override
    public boolean isValid(NamesForm namesForm, ConstraintValidatorContext context) {
        if (namesForm.getOwnerType() != TenantOwnerType.THIRD_PARTY) {
            return true;
        }
        if (StringUtils.isNotBlank(namesForm.getBeneficiaryEmail())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("beneficiaryEmail")
                .addConstraintViolation();
        return false;
    }
}
