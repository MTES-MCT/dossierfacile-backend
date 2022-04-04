package fr.dossierfacile.api.pdfgenerator.service.interfaces;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.enums.FileStatus;

public interface ApartmentSharingService {
    void setDossierPdfDocumentStatus(Long apartmentSharingId,FileStatus inProgress);

    ApartmentSharing initialiseApartmentSharingForPdfGeneration(Long apartmentSharingId);
}
