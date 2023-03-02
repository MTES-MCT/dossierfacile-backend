package fr.dossierfacile.api.front.security;

import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {
    TenantPermissionsService tenantPermissionsService;

    public CustomMethodSecurityExpressionRoot(Authentication authentication, TenantPermissionsService tenantPermissionsService) {
        super(authentication);
        this.tenantPermissionsService = tenantPermissionsService;
    }

    private String getKeycloakId() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSubject();
    }

    private String getKeycloakClientId() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getClaimAsString("azp");
    }

    public boolean hasPermissionOnTenant(Long tenantId) {
        return tenantPermissionsService.canAccess(getKeycloakId(), tenantId);
    }


    public boolean clientHasPermissionOnTenant(Long tenantId) {
        return tenantPermissionsService.clientCanAccess(getKeycloakClientId(), tenantId);
    }

    @Override
    public Object getFilterObject() {
        return null;
    }

    @Override
    public void setFilterObject(Object o) {

    }

    @Override
    public Object getReturnObject() {
        return null;
    }

    @Override
    public void setReturnObject(Object o) {

    }

    @Override
    public Object getThis() {
        return null;
    }

}