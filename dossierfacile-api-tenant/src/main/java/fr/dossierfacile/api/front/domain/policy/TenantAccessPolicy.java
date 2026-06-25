package fr.dossierfacile.api.front.domain.policy;

import fr.dossierfacile.api.front.application.exception.UnauthorizedException;
import fr.dossierfacile.common.domain.model.apartment_sharing.ApartmentSharing;
import fr.dossierfacile.common.domain.model.tenant.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import org.springframework.stereotype.Component;

@Component
public class TenantAccessPolicy {

    public void validateAccess(Tenant currentTenant, Tenant targetTenant, ApartmentSharing apartmentSharing) {

        // Si un tenant tente de modifier un apartmentSharing qui ne lui appartient pas
        if (!currentTenant.getApartmentSharingId().equals(apartmentSharing.getId())) {
            throw new UnauthorizedException("Tenant with id : " + currentTenant.getId() + " does not have access to apartment sharing with id : " + apartmentSharing.getId());
        }

        // Si le tenant édite son propre dossier
        if (currentTenant.getId().equals(targetTenant.getId())) {
            return;
        }

        // Si un tenant tente de modifier un autre tenant qui ne lui appartient pas
        if (!currentTenant.getApartmentSharingId().equals(targetTenant.getApartmentSharingId())) {
            throw new UnauthorizedException("Tenant with id : " + currentTenant.getId() + " does not have access to tenant with id : " + targetTenant.getId());
        }

        // Si on est en coloc, on n'a pas le droit de modifier les informations de son colloc
        // Seule les membres d'un couple peuvent modifier les informations d'un autre tenant'
        if (apartmentSharing.getApplicationType() != ApplicationType.COUPLE) {
            throw new UnauthorizedException("Tenant with id : " + currentTenant.getId() + " does not have access to tenant with id : " + targetTenant.getId());
        }
    }
}
