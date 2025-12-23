package fr.dossierfacile.api.front.validator.tenant.name;

import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.annotation.tenant.name.CheckFranceConnect;
import fr.dossierfacile.common.enums.TenantOwnerType;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CheckFranceConnectValidator extends TenantConstraintValidator<CheckFranceConnect, NamesForm> {

    @Override
    public boolean isValid(NamesForm namesForm, ConstraintValidatorContext constraintValidatorContext) {
        var tenant = getTenant(namesForm);
        // If tenant is not FranceConnect, validation passes
        if (!Boolean.TRUE.equals(tenant.getFranceConnect())) {
            return true;
        }
        // If ownerType is THIRD_PARTY, validation passes
        if (namesForm.getOwnerType() == TenantOwnerType.THIRD_PARTY) {
            return true;
        }
        // If ownerType is SELF, check FranceConnect data
        if (namesForm.getOwnerType() == TenantOwnerType.SELF) {
            // If FranceConnect information is missing (null), validation passes
            // We use userFirstName because the FranceConnect information are stored in the user and not in the tenant
            if (tenant.getUserFirstName() == null || tenant.getUserLastName() == null) {
                return true;
            }
            // Otherwise, verify that the names match
            return Objects.equals(tenant.getUserFirstName(), namesForm.getFirstName()) 
                    && Objects.equals(tenant.getUserLastName(), namesForm.getLastName());
        }
        return true;
    }
}
