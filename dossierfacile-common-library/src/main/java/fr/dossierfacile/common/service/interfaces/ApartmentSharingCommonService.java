package fr.dossierfacile.common.service.interfaces;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;

public interface ApartmentSharingCommonService {
    void resetDossierPdfGenerated(ApartmentSharing apartmentSharing);
    void removeTenant(ApartmentSharing apartmentSharing, Tenant tenant);

    /**
     * Delete apartmentSharing (delete tenants on cascade)
     */
    void delete(ApartmentSharing apartmentSharing);
}
