package fr.dossierfacile.api.front.security;

import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.function.Predicate;

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
        Tenant tenant = tenantService.findByKeycloakId(getKeycloakId());
        TenantPermissions permissions = new TenantPermissions(tenant);
        return permissions.canAccess(tenantId);
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