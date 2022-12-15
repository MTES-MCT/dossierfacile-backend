package fr.dossierfacile.api.front.security;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TenantPermissions {

    private final Tenant tenant;

    public boolean canAccess(Long requestedTenantId) {
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

    private static boolean isMemberOfApartmentSharing(Long tenantId, ApartmentSharing apartmentSharing) {
        return apartmentSharing.getTenants().stream()
                .anyMatch(tenant -> tenantId.equals(tenant.getId()));
    }

}
