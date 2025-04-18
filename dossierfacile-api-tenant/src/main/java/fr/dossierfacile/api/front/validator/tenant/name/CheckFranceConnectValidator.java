package fr.dossierfacile.api.front.validator.tenant.name;

import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.annotation.tenant.name.CheckFranceConnect;
import fr.dossierfacile.common.enums.TenantOwnerType;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckFranceConnectValidator extends TenantConstraintValidator<CheckFranceConnect, NamesForm> {

    @Override
    public boolean isValid(NamesForm namesForm, ConstraintValidatorContext constraintValidatorContext) {
        var tenant = getTenant(namesForm);
        return (!Boolean.TRUE.equals(tenant.getFranceConnect()) ||
                (tenant.getFranceConnect() && namesForm.getOwnerType() == TenantOwnerType.THIRD_PARTY) ||
                (tenant.getFranceConnect() && namesForm.getOwnerType() == TenantOwnerType.SELF &&
                        // We use userFirstName because the FranceConnect information are stored in the user and not in the tenant
                        tenant.getUserFirstName().equals(namesForm.getFirstName()) && tenant.getUserLastName().equals(namesForm.getLastName())));
    }
}
