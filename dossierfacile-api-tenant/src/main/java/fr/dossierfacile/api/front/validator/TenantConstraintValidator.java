package fr.dossierfacile.api.front.validator;

import fr.dossierfacile.api.front.form.interfaces.FormWithTenantId;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Tenant;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import java.lang.annotation.Annotation;

public abstract class TenantConstraintValidator<A extends Annotation, F extends FormWithTenantId> implements ConstraintValidator<A, F> {
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private TenantService tenantService;

    @Override
    public void initialize(A constraintAnnotation) {
    }

    protected Tenant getTenant(F form) {
        return (form.getTenantId() == null) ? authenticationFacade.getLoggedTenant() : tenantService.findById(form.getTenantId());
    }
}
