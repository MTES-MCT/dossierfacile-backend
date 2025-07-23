package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.MappingFormat;
import fr.dossierfacile.api.front.model.tenant.FullFolderFile;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApartmentSharingService {

    ApplicationModel full(String token);

    ApplicationModel light(String token);

    FullFolderFile downloadFullPdf(String token) throws IOException;

    void resetDossierPdfGenerated(ApartmentSharing apartmentSharing);

    void createFullPdf(String token);

    Optional<ApartmentSharing> findById(Long apartmentSharingId);

    List<ApplicationModel> findApartmentSharingByLastUpdateDateAndPartner(LocalDateTime lastUpdateDate, UserApi userApi, int limit, MappingFormat format);

    void refreshUpdateDate(ApartmentSharing apartmentSharing);

    void removeTenant(ApartmentSharing apartmentSharing, Tenant tenant);

    /**
     * Delete apartmentSharing (delete tenants on cascade)
     */
    void delete(ApartmentSharing apartmentSharing);

    FullFolderFile zipDocuments(Tenant tenant);
}
