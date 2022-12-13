package fr.dossierfacile.api.front.security;

import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {
    TenantService tenantService;

    public CustomMethodSecurityExpressionRoot(Authentication authentication, TenantService tenantService) {
        super(authentication);
        this.tenantService = tenantService;
    }

    private String getKeycloakId() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSubject();
    }

    public boolean hasPermissionOnTenant(Long tenantId) {
        if (tenantId == null)
            return true;
        Tenant tenant = tenantService.findByKeycloakId(getKeycloakId());
        return tenant.getApartmentSharing().getTenants().stream().anyMatch(co -> tenantId.equals(co.getId()) );
    }
    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }
    @Override
    public void setFilterObject(Object o) {

    }

    @Override
    public Object getFilterObject() {
        return null;
    }

    @Override
    public void setReturnObject(Object o) {

    }

    @Override
    public Object getReturnObject() {
        return null;
    }

    @Override
    public Object getThis() {
        return null;
    }

}