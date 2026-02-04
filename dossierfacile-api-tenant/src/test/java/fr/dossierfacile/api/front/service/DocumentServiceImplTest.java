package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.model.tenant.AnalysisStatus;
import fr.dossierfacile.api.front.model.tenant.DocumentAnalysisStatusResponse;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.repository.DocumentIAFileAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentAnalysisReportRepository documentAnalysisReportRepository;

    @Mock
    private DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private Document document;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        ApartmentSharing apartmentSharing = ApartmentSharing.builder()
                .id(1L)
                .build();

        tenant = Tenant.builder()
                .id(1L)
                .apartmentSharing(apartmentSharing)
                .build();

        document = Document.builder()
                .id(1L)
                .tenant(tenant)
                .build();
    }

    @Test
    void shouldReturnCompletedStatusWhenAllFilesAreAnalyzed() {
        // Given
        DocumentAnalysisReport report = DocumentAnalysisReport.builder()
                .id(1L)
                .analysisStatus(DocumentAnalysisStatus.CHECKED)
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentIAFileAnalysisRepository.countTotalFilesByDocumentId(1L)).thenReturn(5L);
        when(documentIAFileAnalysisRepository.countAnalyzedFilesByDocumentId(1L)).thenReturn(5L);
        when(documentAnalysisReportRepository.findByDocumentId(1L)).thenReturn(Optional.of(report));

        // When
        DocumentAnalysisStatusResponse response = documentService.getDocumentAnalysisStatus(1L, tenant);

        // Then
        assertThat(response.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(response.getAnalysisReport()).isNotNull();
        assertThat(response.getAnalysisReport().getId()).isEqualTo(1L);
        assertThat(response.getAnalyzedFiles()).isNull();
        assertThat(response.getTotalFiles()).isNull();
    }

    @Test
    void shouldReturnCompletedStatusWithoutReportWhenAllFilesAreAnalyzedButNoReportExists() {
        // Given
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentIAFileAnalysisRepository.countTotalFilesByDocumentId(1L)).thenReturn(3L);
        when(documentIAFileAnalysisRepository.countAnalyzedFilesByDocumentId(1L)).thenReturn(3L);
        when(documentAnalysisReportRepository.findByDocumentId(1L)).thenReturn(Optional.empty());

        // When
        DocumentAnalysisStatusResponse response = documentService.getDocumentAnalysisStatus(1L, tenant);

        // Then
        assertThat(response.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(response.getAnalysisReport()).isNull();
        assertThat(response.getAnalyzedFiles()).isNull();
        assertThat(response.getTotalFiles()).isNull();
    }

    @Test
    void shouldReturnNoAnalysisScheduledWhenTotalFilesIsZero() {
        // Given
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentIAFileAnalysisRepository.countTotalFilesByDocumentId(1L)).thenReturn(0L);

        // When
        DocumentAnalysisStatusResponse response = documentService.getDocumentAnalysisStatus(1L, tenant);

        // Then
        assertThat(response.getStatus()).isEqualTo(AnalysisStatus.NO_ANALYSIS_SCHEDULED);
        assertThat(response.getAnalysisReport()).isNull();
        assertThat(response.getAnalyzedFiles()).isNull();
        assertThat(response.getTotalFiles()).isNull();
    }

    @Test
    void shouldReturnInProgressWhenFilesAreBeingAnalyzed() {
        // Given
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentIAFileAnalysisRepository.countTotalFilesByDocumentId(1L)).thenReturn(5L);
        when(documentIAFileAnalysisRepository.countAnalyzedFilesByDocumentId(1L)).thenReturn(2L);

        // When
        DocumentAnalysisStatusResponse response = documentService.getDocumentAnalysisStatus(1L, tenant);

        // Then
        assertThat(response.getStatus()).isEqualTo(AnalysisStatus.IN_PROGRESS);
        assertThat(response.getAnalyzedFiles()).isEqualTo(2);
        assertThat(response.getTotalFiles()).isEqualTo(5);
        assertThat(response.getAnalysisReport()).isNull();
    }

    @Test
    void shouldThrowDocumentNotFoundExceptionWhenDocumentDoesNotExist() {
        // Given
        when(documentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(DocumentNotFoundException.class, () -> 
            documentService.getDocumentAnalysisStatus(999L, tenant)
        );
    }

    @Test
    void shouldThrowAccessDeniedExceptionWhenTenantDoesNotOwnDocument() {
        // Given
        Tenant otherTenant = Tenant.builder()
                .id(2L)
                .apartmentSharing(ApartmentSharing.builder().id(2L).build())
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> 
            documentService.getDocumentAnalysisStatus(1L, otherTenant)
        );
    }
}
