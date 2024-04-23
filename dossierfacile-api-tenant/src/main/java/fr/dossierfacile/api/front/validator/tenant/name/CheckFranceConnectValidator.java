package fr.dossierfacile.api.front.validator.tenant.name;

import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.annotation.tenant.name.CheckFranceConnect;
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
                (tenant.getFirstName().equals(namesForm.getFirstName()) && tenant.getLastName().equals(namesForm.getLastName())));
    }
}
