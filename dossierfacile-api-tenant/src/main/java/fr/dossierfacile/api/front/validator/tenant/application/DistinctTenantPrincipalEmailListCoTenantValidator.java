package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.DistinctTenantPrincipalEmailListCoTenant;
import fr.dossierfacile.common.entity.Tenant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

@Component
@AllArgsConstructor
public class DistinctTenantPrincipalEmailListCoTenantValidator implements ConstraintValidator<DistinctTenantPrincipalEmailListCoTenant, List<String>> {

    private final AuthenticationFacade authenticationFacade;

    @Override
    public void initialize(DistinctTenantPrincipalEmailListCoTenant constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(List<String> emails, ConstraintValidatorContext constraintValidatorContext) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        return !emails.contains(tenant.getEmail());
    }
}
