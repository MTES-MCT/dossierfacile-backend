package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.StorageFile;

public interface ApartmentSharingPdfDossierFileGenerationService {
    ApartmentSharing initialize(Long apartmentSharingId);

    void fail(Long apartmentSharingId, StorageFile pdfFile);

    void complete(Long apartmentSharingId, StorageFile pdfFile);
}