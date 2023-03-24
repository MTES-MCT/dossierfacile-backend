package fr.dossierfacile.api.front.validator.tenant.name;

import fr.dossierfacile.api.front.register.form.tenant.NamesForm;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.anotation.tenant.name.CheckFranceConnect;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@RequiredArgsConstructor
public class CheckFranceConnectValidator implements ConstraintValidator<CheckFranceConnect, NamesForm> {

    private final TenantService tenantService;

    @Override
    public void initialize(CheckFranceConnect constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(NamesForm namesForm, ConstraintValidatorContext constraintValidatorContext) {
        var tenant = tenantService.findById(namesForm.getTenantId());
        return (!Boolean.TRUE.equals(tenant.getFranceConnect()) ||
                (tenant.getFirstName().equals(namesForm.getFirstName()) && tenant.getLastName().equals(namesForm.getLastName())));
    }
}
