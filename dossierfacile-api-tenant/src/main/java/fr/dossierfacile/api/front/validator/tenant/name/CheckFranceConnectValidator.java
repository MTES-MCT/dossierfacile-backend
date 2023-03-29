package fr.dossierfacile.api.front.validator.tenant.name;

import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.validator.TenantConstraintValidator;
import fr.dossierfacile.api.front.validator.anotation.tenant.name.CheckFranceConnect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidatorContext;

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
