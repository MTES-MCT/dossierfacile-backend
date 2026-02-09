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

import fr.dossierfacile.common.enums.ApplicationType;

import java.util.List;
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

    // --- Permission tests ---

    @Test
    void shouldAllowAccessToGuarantorDocument() {
        // Given
        Guarantor guarantor = Guarantor.builder().id(10L).tenant(tenant).build();
        Document guarantorDoc = Document.builder().id(2L).guarantor(guarantor).build();

        when(documentRepository.findById(2L)).thenReturn(Optional.of(guarantorDoc));
        when(documentIAFileAnalysisRepository.countTotalFilesByDocumentId(2L)).thenReturn(0L);

        // When
        DocumentAnalysisStatusResponse response = documentService.getDocumentAnalysisStatus(2L, tenant);

        // Then
        assertThat(response.getStatus()).isEqualTo(AnalysisStatus.NO_ANALYSIS_SCHEDULED);
    }

    @Test
    void shouldAllowAccessToPartnerDocumentInCouple() {
        // Given
        Tenant partner = Tenant.builder().id(2L).build();
        ApartmentSharing coupleSharing = ApartmentSharing.builder()
                .id(1L)
                .applicationType(ApplicationType.COUPLE)
                .tenants(List.of(tenant, partner))
                .build();
        tenant.setApartmentSharing(coupleSharing);
        partner.setApartmentSharing(coupleSharing);

        Document partnerDoc = Document.builder().id(3L).tenant(partner).build();

        when(documentRepository.findById(3L)).thenReturn(Optional.of(partnerDoc));
        when(documentIAFileAnalysisRepository.countTotalFilesByDocumentId(3L)).thenReturn(0L);

        // When
        DocumentAnalysisStatusResponse response = documentService.getDocumentAnalysisStatus(3L, tenant);

        // Then
        assertThat(response.getStatus()).isEqualTo(AnalysisStatus.NO_ANALYSIS_SCHEDULED);
    }

    @Test
    void shouldAllowAccessToPartnerGuarantorDocumentInCouple() {
        // Given
        Tenant partner = Tenant.builder().id(2L).build();
        ApartmentSharing coupleSharing = ApartmentSharing.builder()
                .id(1L)
                .applicationType(ApplicationType.COUPLE)
                .tenants(List.of(tenant, partner))
                .build();
        tenant.setApartmentSharing(coupleSharing);
        partner.setApartmentSharing(coupleSharing);

        Guarantor partnerGuarantor = Guarantor.builder().id(20L).tenant(partner).build();
        Document partnerGuarantorDoc = Document.builder().id(4L).guarantor(partnerGuarantor).build();

        when(documentRepository.findById(4L)).thenReturn(Optional.of(partnerGuarantorDoc));
        when(documentIAFileAnalysisRepository.countTotalFilesByDocumentId(4L)).thenReturn(0L);

        // When
        DocumentAnalysisStatusResponse response = documentService.getDocumentAnalysisStatus(4L, tenant);

        // Then
        assertThat(response.getStatus()).isEqualTo(AnalysisStatus.NO_ANALYSIS_SCHEDULED);
    }

    @Test
    void shouldDenyAccessToCoTenantDocumentInGroup() {
        // Given
        Tenant coTenant = Tenant.builder().id(3L).build();
        ApartmentSharing groupSharing = ApartmentSharing.builder()
                .id(1L)
                .applicationType(ApplicationType.GROUP)
                .tenants(List.of(tenant, coTenant))
                .build();
        tenant.setApartmentSharing(groupSharing);

        Document coTenantDoc = Document.builder().id(5L).tenant(coTenant).build();

        when(documentRepository.findById(5L)).thenReturn(Optional.of(coTenantDoc));

        // When & Then
        assertThrows(AccessDeniedException.class, () ->
            documentService.getDocumentAnalysisStatus(5L, tenant)
        );
    }

    @Test
    void shouldDenyAccessToCoTenantGuarantorDocumentInGroup() {
        // Given
        Tenant coTenant = Tenant.builder().id(3L).build();
        ApartmentSharing groupSharing = ApartmentSharing.builder()
                .id(1L)
                .applicationType(ApplicationType.GROUP)
                .tenants(List.of(tenant, coTenant))
                .build();
        tenant.setApartmentSharing(groupSharing);

        Guarantor coTenantGuarantor = Guarantor.builder().id(30L).tenant(coTenant).build();
        Document coTenantGuarantorDoc = Document.builder().id(6L).guarantor(coTenantGuarantor).build();

        when(documentRepository.findById(6L)).thenReturn(Optional.of(coTenantGuarantorDoc));

        // When & Then
        assertThrows(AccessDeniedException.class, () ->
            documentService.getDocumentAnalysisStatus(6L, tenant)
        );
    }

    @Test
    void shouldDenyAccessToUnrelatedTenantDocument() {
        // Given
        Tenant unrelatedTenant = Tenant.builder().id(99L)
                .apartmentSharing(ApartmentSharing.builder().id(99L).build())
                .build();
        Document unrelatedDoc = Document.builder().id(7L).tenant(unrelatedTenant).build();

        when(documentRepository.findById(7L)).thenReturn(Optional.of(unrelatedDoc));

        // When & Then
        assertThrows(AccessDeniedException.class, () ->
            documentService.getDocumentAnalysisStatus(7L, tenant)
        );
    }
}
