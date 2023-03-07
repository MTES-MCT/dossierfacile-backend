package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.repository.TenantUserApiRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TenantPermissionsServiceImpl implements TenantPermissionsService {
    private final TenantService tenantService;
    private final TenantUserApiRepository tenantUserApiRepository;

    public boolean clientCanAccess(String keycloakClientId, Long tenantId) {
        return tenantUserApiRepository.existsByUserApiNameAndTenantId(keycloakClientId, tenantId);
    }

    public boolean canAccess(String keycloakUserId, Long requestedTenantId) {
        Tenant tenant = tenantService.findByKeycloakId(keycloakUserId);
        if (requestedTenantId == null) {
            return true;
        }

        if (requestedTenantId.equals(tenant.getId())) {
            return true;
        }

        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        ApplicationType applicationType = apartmentSharing.getApplicationType();
        if (applicationType == ApplicationType.COUPLE) {
            return isMemberOfApartmentSharing(requestedTenantId, apartmentSharing);
        }

        return false;
    }

    private boolean isMemberOfApartmentSharing(Long tenantId, ApartmentSharing apartmentSharing) {
        return apartmentSharing.getTenants().stream()
                .anyMatch(tenant -> tenantId.equals(tenant.getId()));
    }

}
