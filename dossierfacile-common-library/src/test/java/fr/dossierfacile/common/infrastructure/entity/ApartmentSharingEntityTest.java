package fr.dossierfacile.common.infrastructure.entity;

import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStorageStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApartmentSharingEntityTest {

    @Test
    void should_mark_pdf_dossier_file_as_to_delete_on_pre_remove() {
        StorageFile storageFile = StorageFile.builder().status(FileStorageStatus.TEMPORARY).build();
        ApartmentSharingEntity entity = ApartmentSharingEntity.builder()
                .pdfDossierFile(storageFile)
                .build();

        entity.deleteCascade();

        assertThat(storageFile.getStatus()).isEqualTo(FileStorageStatus.TO_DELETE);
    }
}
