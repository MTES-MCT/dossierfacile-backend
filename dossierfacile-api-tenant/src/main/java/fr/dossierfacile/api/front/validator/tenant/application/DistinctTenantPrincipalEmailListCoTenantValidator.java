package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.DistinctTenantPrincipalEmailListCoTenant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
@AllArgsConstructor
public class DistinctTenantPrincipalEmailListCoTenantValidator implements ConstraintValidator<DistinctTenantPrincipalEmailListCoTenant, ApplicationForm> {

    private final AuthenticationFacade authenticationFacade;

    @Override
    public void initialize(DistinctTenantPrincipalEmailListCoTenant constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(ApplicationForm applicationForm, ConstraintValidatorContext constraintValidatorContext) {
        var tenant = authenticationFacade.getTenant(applicationForm.getTenantId());
        if (tenant == null) {
            return true;
        }
        var isValid = !applicationForm.getCoTenantEmail().contains(tenant.getEmail());
        if (!isValid) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate(constraintValidatorContext.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("coTenantEmail").addConstraintViolation();
        }
        return isValid;
    }
}
