package fr.dossierfacile.api.front.validator.tenant.application;

import fr.dossierfacile.api.front.register.form.tenant.ApplicationForm;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.api.front.validator.anotation.tenant.application.DeniedJoinTenant;
import fr.dossierfacile.common.enums.TenantType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/** deprecated  since 202209 */
@Deprecated
@Component
@RequiredArgsConstructor
public class DeniedJoinTenantValidator implements ConstraintValidator<DeniedJoinTenant, ApplicationForm> {
    private final TenantService tenantService;

    @Override
    public void initialize(DeniedJoinTenant constraintAnnotation) {
        //this method is empty
    }

    @Override
    public boolean isValid(ApplicationForm applicationForm, ConstraintValidatorContext constraintValidatorContext) {
        var tenant = tenantService.findById(applicationForm.getTenantId());
        if (tenant == null) {
            return true;
        }
        return tenant.getTenantType() != TenantType.JOIN;
    }
}
