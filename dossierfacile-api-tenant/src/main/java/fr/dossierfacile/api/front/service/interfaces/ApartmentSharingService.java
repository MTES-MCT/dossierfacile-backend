package fr.dossierfacile.api.front.service.interfaces;

import fr.dossierfacile.api.front.model.tenant.ApplicationAnalysisStatusResponse;
import fr.dossierfacile.api.front.model.tenant.FullFolderFile;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;

import fr.dossierfacile.common.entity.Document;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public interface ApartmentSharingService {

    void linkExists(UUID token, boolean fullData);

    ApplicationModel full(UUID token, String trigram, Tenant loggedInTenant);

    ApplicationModel full(Tenant tenant);

    ApplicationModel light(UUID token);

    FullFolderFile downloadFullPdf(UUID token) throws IOException;

    void resetDossierPdfGenerated(ApartmentSharing apartmentSharing);

    void createFullPdf(UUID token);

    Optional<ApartmentSharing> findById(Long apartmentSharingId);

    void refreshUpdateDate(ApartmentSharing apartmentSharing);

    void removeTenant(ApartmentSharing apartmentSharing, Tenant tenant);

    /**
     * Delete apartmentSharing (delete tenants on cascade)
     */
    void delete(ApartmentSharing apartmentSharing);

    FullFolderFile zipDocuments(Tenant tenant);

    void createFullPdfForTenant(Tenant tenant);

    FullFolderFile downloadFullPdfForTenant(Tenant tenant) throws IOException;

    ApplicationAnalysisStatusResponse getFullAnalysisStatus(Tenant tenant);

    Document findDocumentByLink(UUID token, String documentName);

}
