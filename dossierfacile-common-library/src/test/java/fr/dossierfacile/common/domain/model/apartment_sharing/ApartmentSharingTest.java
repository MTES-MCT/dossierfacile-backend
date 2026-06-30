package fr.dossierfacile.common.domain.model.apartment_sharing;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.enums.FileStorageStatus;
import fr.dossierfacile.common.infrastructure.entity.ApartmentSharingEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApartmentSharingTest {

    @Test
    void should_reset_pdf_dossier_file_and_set_status_to_to_delete() {
        // Given
        StorageFile storageFile = StorageFile.builder()
                .status(FileStorageStatus.TEMPORARY)
                .build();
        ApartmentSharingEntity entity = ApartmentSharingEntity.builder()
                .pdfDossierFile(storageFile)
                .dossierPdfDocumentStatus(FileStatus.COMPLETED)
                .build();
        ApartmentSharing apartmentSharing = new ApartmentSharing(entity);

        // When
        apartmentSharing.resetDossierPdfGenerated();

        // Then
        assertThat(entity.getPdfDossierFile()).isNull();
        assertThat(entity.getDossierPdfDocumentStatus()).isEqualTo(FileStatus.DELETED);
        assertThat(storageFile.getStatus()).isEqualTo(FileStorageStatus.TO_DELETE);
    }

    @Test
    void should_do_nothing_when_pdf_dossier_file_is_already_null() {
        // Given
        ApartmentSharingEntity entity = ApartmentSharingEntity.builder()
                .pdfDossierFile(null)
                .dossierPdfDocumentStatus(null)
                .build();
        ApartmentSharing apartmentSharing = new ApartmentSharing(entity);

        // When
        apartmentSharing.resetDossierPdfGenerated();

        // Then
        assertThat(entity.getPdfDossierFile()).isNull();
        assertThat(entity.getDossierPdfDocumentStatus()).isNull();
    }
}
