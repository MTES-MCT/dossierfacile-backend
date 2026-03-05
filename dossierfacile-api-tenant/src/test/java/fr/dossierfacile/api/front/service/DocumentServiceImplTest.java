package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.AnalysisStatus;
import fr.dossierfacile.api.front.model.tenant.DocumentAnalysisStatusResponse;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.repository.DocumentIAFileAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository;

    // Unused mocks required for constructor injection
    @Mock
    private fr.dossierfacile.common.repository.DocumentAnalysisReportRepository documentAnalysisReportRepository;
    @Mock
    private fr.dossierfacile.common.service.interfaces.FileStorageService fileStorageService;
    @Mock
    private fr.dossierfacile.api.front.service.interfaces.TenantStatusService tenantStatusService;
    @Mock
    private fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService apartmentSharingService;
    @Mock
    private fr.dossierfacile.common.service.interfaces.DocumentHelperService documentHelperService;
    @Mock
    private fr.dossierfacile.common.service.interfaces.LogService logService;
    @Mock
    private fr.dossierfacile.api.front.amqp.Producer producer;

    private static final String DOCUMENT_NAME = "test-document.pdf";
    @Mock
    private TenantMapper tenantMapper;

    @Nested
    class GetAuthorizedDocument {

        @Nested
        class WhenTenantOwnsTheDocument {
            @Test
            void shouldReturnDocument() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.ALONE);

                Tenant tenant = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                sharing.setTenants(List.of(tenant));

                Document document = Document.builder()
                        .id(1L)
                        .name(DOCUMENT_NAME)
                        .tenant(tenant)
                        .build();

                when(documentRepository.findFirstByName(DOCUMENT_NAME)).thenReturn(Optional.of(document));

                Document result = documentService.getAuthorizedDocument(DOCUMENT_NAME, tenant);

                assertThat(result).isEqualTo(document);
            }
        }

        @Nested
        class WhenDocumentBelongsToTenantGuarantor {
            @Test
            void shouldReturnDocument() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.ALONE);

                Tenant tenant = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                sharing.setTenants(List.of(tenant));

                Guarantor guarantor = Guarantor.builder().id(1L).tenant(tenant).build();

                Document document = Document.builder()
                        .id(1L)
                        .name(DOCUMENT_NAME)
                        .guarantor(guarantor)
                        .build();

                when(documentRepository.findFirstByName(DOCUMENT_NAME)).thenReturn(Optional.of(document));

                Document result = documentService.getAuthorizedDocument(DOCUMENT_NAME, tenant);

                assertThat(result).isEqualTo(document);
            }
        }

        @Nested
        class WhenCouplePartnerDocument {
            @Test
            void shouldReturnDocument() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.COUPLE);

                Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                Tenant tenant2 = Tenant.builder().id(2L).apartmentSharing(sharing).build();
                sharing.setTenants(List.of(tenant1, tenant2));

                Document document = Document.builder()
                        .id(1L)
                        .name(DOCUMENT_NAME)
                        .tenant(tenant2)
                        .build();

                when(documentRepository.findFirstByName(DOCUMENT_NAME)).thenReturn(Optional.of(document));

                Document result = documentService.getAuthorizedDocument(DOCUMENT_NAME, tenant1);

                assertThat(result).isEqualTo(document);
            }
        }

        @Nested
        class WhenCouplePartnerGuarantorDocument {
            @Test
            void shouldReturnDocument() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.COUPLE);

                Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                Tenant tenant2 = Tenant.builder().id(2L).apartmentSharing(sharing).build();
                sharing.setTenants(List.of(tenant1, tenant2));

                Guarantor guarantor = Guarantor.builder().id(1L).tenant(tenant2).build();

                Document document = Document.builder()
                        .id(1L)
                        .name(DOCUMENT_NAME)
                        .guarantor(guarantor)
                        .build();

                when(documentRepository.findFirstByName(DOCUMENT_NAME)).thenReturn(Optional.of(document));

                Document result = documentService.getAuthorizedDocument(DOCUMENT_NAME, tenant1);

                assertThat(result).isEqualTo(document);
            }
        }

        @Nested
        class WhenAloneAndAnotherTenantDocument {
            @Test
            void shouldThrowAccessDeniedException() {
                ApartmentSharing sharing1 = new ApartmentSharing();
                sharing1.setId(1L);
                sharing1.setApplicationType(ApplicationType.ALONE);

                Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharing1).build();
                sharing1.setTenants(List.of(tenant1));

                Tenant otherTenant = Tenant.builder().id(99L).build();

                Document document = Document.builder()
                        .id(1L)
                        .name(DOCUMENT_NAME)
                        .tenant(otherTenant)
                        .build();

                when(documentRepository.findFirstByName(DOCUMENT_NAME)).thenReturn(Optional.of(document));

                assertThrows(AccessDeniedException.class, () ->
                        documentService.getAuthorizedDocument(DOCUMENT_NAME, tenant1)
                );
            }
        }

        @Nested
        class WhenDocumentDoesNotExist {
            @Test
            void shouldThrowDocumentNotFoundException() {
                Tenant tenant = Tenant.builder().id(1L).build();

                when(documentRepository.findFirstByName(DOCUMENT_NAME)).thenReturn(Optional.empty());

                assertThrows(DocumentNotFoundException.class, () ->
                        documentService.getAuthorizedDocument(DOCUMENT_NAME, tenant)
                );
            }
        }
    }

    @Nested
    class AnalysisTests {

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
}
