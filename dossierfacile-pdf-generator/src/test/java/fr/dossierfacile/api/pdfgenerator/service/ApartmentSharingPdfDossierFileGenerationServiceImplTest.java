package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApartmentSharingPdfDossierFileGenerationServiceImplTest {

    private ApartmentSharingCommonService apartmentSharingCommonService;
    private FileStorageService fileStorageService;

    private ApartmentSharingPdfDossierFileGenerationServiceImpl service;

    private StorageFile generatedFile;

    @BeforeEach
    void setUp() {
        apartmentSharingCommonService = mock(ApartmentSharingCommonService.class);
        fileStorageService = mock(FileStorageService.class);
        service = new ApartmentSharingPdfDossierFileGenerationServiceImpl(apartmentSharingCommonService, fileStorageService);
        generatedFile = StorageFile.builder().id(42L).build();
    }

    @Test
    void complete_attachesFile_whenGenerationIsStillInProgress() {
        ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .dossierPdfDocumentStatus(FileStatus.IN_PROGRESS)
                .build();
        when(apartmentSharingCommonService.findById(1L)).thenReturn(Optional.of(apartmentSharing));

        service.complete(1L, generatedFile);

        assertThat(apartmentSharing.getPdfDossierFile()).isEqualTo(generatedFile);
        assertThat(apartmentSharing.getDossierPdfDocumentStatus()).isEqualTo(FileStatus.COMPLETED);
        verify(apartmentSharingCommonService).save(apartmentSharing);
        verify(fileStorageService, never()).delete(any(StorageFile.class));
    }

    @Test
    void complete_discardsFile_whenGenerationWasInvalidatedDuringRendering() {
        // The dossier changed or left the COMPLETED/VALIDATED state while the PDF
        // was being rendered: resetDossierPdfGenerated() set the status to DELETED
        ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .dossierPdfDocumentStatus(FileStatus.DELETED)
                .build();
        when(apartmentSharingCommonService.findById(1L)).thenReturn(Optional.of(apartmentSharing));

        service.complete(1L, generatedFile);

        assertThat(apartmentSharing.getPdfDossierFile()).isNull();
        assertThat(apartmentSharing.getDossierPdfDocumentStatus()).isEqualTo(FileStatus.DELETED);
        verify(apartmentSharingCommonService, never()).save(any(ApartmentSharing.class));
        verify(fileStorageService).delete(generatedFile);
    }

    @Test
    void complete_discardsFile_whenApartmentSharingWasDeleted() {
        when(apartmentSharingCommonService.findById(1L)).thenReturn(Optional.empty());

        service.complete(1L, generatedFile);

        verify(apartmentSharingCommonService, never()).save(any(ApartmentSharing.class));
        verify(fileStorageService).delete(generatedFile);
    }
}
