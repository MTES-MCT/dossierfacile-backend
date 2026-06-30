package fr.dossierfacile.common.domain.model.apartment_sharing;

import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.enums.FileStorageStatus;
import fr.dossierfacile.common.infrastructure.entity.ApartmentSharingEntity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregate Root pour le concept d'ApartmentSharing (Dossier de candidature).
 */
@SuppressWarnings("ClassCanBeRecord")
public class ApartmentSharing implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ApartmentSharingEntity entity;

    public ApartmentSharing(ApartmentSharingEntity entity) {
        this.entity = entity;
    }

    public ApartmentSharingEntity getEntity() {
        return this.entity;
    }

    // --- ACCESSEURS (LECTURE SEULE POUR PROJECTIONS ET USE CASES) ---

    public Long getId() {
        return entity.getId();
    }

    public ApplicationType getApplicationType() {
        return entity.getApplicationType();
    }

    public LocalDateTime getLastUpdateDate() {
        return entity.getLastUpdateDate();
    }

    public List<Long> getTenantIds() {
        return entity.getTenantIds();
    }

    public void resetDossierPdfGenerated() {
        if (entity.getPdfDossierFile() != null) {
            entity.getPdfDossierFile().setStatus(FileStorageStatus.TO_DELETE);
            entity.setPdfDossierFile(null);
            entity.setDossierPdfDocumentStatus(FileStatus.DELETED);
        }
    }
}
