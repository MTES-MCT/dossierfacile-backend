package fr.dossierfacile.api.front.service.interfaces;

public interface TenantPermissionsService {
    boolean clientCanAccess(String keycloakClientId, Long tenantId);

    boolean canAccess(String keycloakUserId, Long requestedTenantId);
}
