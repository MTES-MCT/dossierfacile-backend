package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.exception.ApartmentSharingUnexpectedException;
import fr.dossierfacile.api.front.model.tenant.AnalysisStatus;
import fr.dossierfacile.api.front.model.tenant.DocumentAnalysisStatusResponse;
import fr.dossierfacile.api.front.model.tenant.FullFolderFile;
import fr.dossierfacile.api.front.repository.ApiTenantLogRepository;
import fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.FileStatus;
import fr.dossierfacile.common.enums.LogType;
import fr.dossierfacile.common.mapper.ApplicationBasicMapper;
import fr.dossierfacile.common.mapper.ApplicationFullMapper;
import fr.dossierfacile.common.mapper.ApplicationLightMapper;
import fr.dossierfacile.common.repository.ApartmentSharingLinkRepository;
import fr.dossierfacile.common.repository.ApartmentSharingRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import fr.dossierfacile.common.service.interfaces.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ApartmentSharingServiceImpl.class})
class ApartmentSharingServiceImplTest {

    @Autowired
    private ApartmentSharingServiceImpl apartmentSharingService;

    @MockitoBean
    private ApartmentSharingRepository apartmentSharingRepository;
    @MockitoBean
    private ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    @MockitoBean
    private TenantCommonRepository tenantRepository;
    @MockitoBean
    private ApplicationFullMapper applicationFullMapper;
    @MockitoBean
    private ApplicationLightMapper applicationLightMapper;
    @MockitoBean
    private ApplicationBasicMapper applicationBasicMapper;
    @MockitoBean
    private FileStorageService fileStorageService;
    @MockitoBean
    private LinkLogService linkLogService;
    @MockitoBean
    private Producer producer;
    @MockitoBean
    private ApartmentSharingCommonService apartmentSharingCommonService;
    @MockitoBean
    private ApiTenantLogRepository tenantLogRepository;
    @MockitoBean
    private LogService logService;
    @MockitoBean
    private BruteForceProtectionService bruteForceProtectionService;
    @MockitoBean
    private TenantPermissionsService tenantPermissionsService;
    @MockitoBean
    private DocumentService documentService;

    private Tenant tenant;
    private ApartmentSharing apartmentSharing;
    private StorageFile pdfFile;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setFirstName("John");
        tenant.setLastName("Doe");

        apartmentSharing = new ApartmentSharing();
        apartmentSharing.setId(1L);
        apartmentSharing.setApplicationType(ApplicationType.ALONE);

        pdfFile = new StorageFile();
        pdfFile.setName("dossier.pdf");
        pdfFile.setPath("/path/to/dossier.pdf");

        tenant.setApartmentSharing(apartmentSharing);
    }

    @Nested
    class DownloadFullPdfForTenant {

        @Nested
        class WhenStatusIsCompleted {
            @Test
            void shouldReturnPdfFile() throws IOException {
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.COMPLETED);
                apartmentSharing.setPdfDossierFile(pdfFile);

                byte[] pdfContent = "PDF content".getBytes();
                InputStream inputStream = new ByteArrayInputStream(pdfContent);
                when(fileStorageService.download(pdfFile)).thenReturn(inputStream);

                FullFolderFile result = apartmentSharingService.downloadFullPdfForTenant(tenant);

                assertThat(result).isNotNull();
                assertThat(result.getFileOutputStream()).isNotNull();
                assertThat(result.getFileOutputStream().toByteArray()).isEqualTo(pdfContent);
                verify(logService).saveLog(eq(LogType.PDF_DOWNLOAD), eq(tenant.getId()));
            }
        }

        @Nested
        class WhenNoApartmentSharing {
            @Test
            void shouldThrowApartmentSharingNotFoundException() {
                tenant.setApartmentSharing(null);

                assertThrows(ApartmentSharingNotFoundException.class, () ->
                        apartmentSharingService.downloadFullPdfForTenant(tenant)
                );
            }
        }

        @Nested
        class WhenStatusIsInProgress {
            @Test
            void shouldThrowIllegalStateException() {
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.IN_PROGRESS);

                assertThrows(IllegalStateException.class, () ->
                        apartmentSharingService.downloadFullPdfForTenant(tenant)
                );
            }
        }

        @Nested
        class WhenStatusIsFailed {
            @Test
            void shouldThrowFileNotFoundException() {
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.FAILED);

                assertThrows(FileNotFoundException.class, () ->
                        apartmentSharingService.downloadFullPdfForTenant(tenant)
                );
            }
        }

        @Nested
        class WhenStatusIsNone {
            @Test
            void shouldThrowExceptionButStillTriggerPdfCreation() {
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.NONE);
                when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(anyLong())).thenReturn(0);

                assertThrows(IllegalStateException.class, () ->
                        apartmentSharingService.downloadFullPdfForTenant(tenant)
                );

                verify(producer).generateFullPdf(apartmentSharing.getId());
            }
        }

        @Nested
        class WhenStatusIsNull {
            @Test
            void shouldThrowExceptionButStillTriggerPdfCreation() {
                apartmentSharing.setDossierPdfDocumentStatus(null);
                when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(anyLong())).thenReturn(0);

                assertThrows(IllegalStateException.class, () ->
                        apartmentSharingService.downloadFullPdfForTenant(tenant)
                );

                verify(producer).generateFullPdf(apartmentSharing.getId());
            }
        }
    }

    @Nested
    class CreateFullPdfForTenant {

        @Nested
        class WhenNoApartmentSharing {
            @Test
            void shouldThrowApartmentSharingNotFoundException() {
                tenant.setApartmentSharing(null);

                assertThrows(ApartmentSharingNotFoundException.class, () ->
                        apartmentSharingService.createFullPdfForTenant(tenant)
                );
            }
        }

        @Nested
        class WhenStatusIsCompleted {
            @Test
            void shouldNotTriggerPdfGeneration() {
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.COMPLETED);
                when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(anyLong())).thenReturn(0);

                apartmentSharingService.createFullPdfForTenant(tenant);

                verify(producer, org.mockito.Mockito.never()).generateFullPdf(anyLong());
            }
        }

        @Nested
        class WhenStatusIsInProgress {
            @Test
            void shouldNotTriggerPdfGeneration() {
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.IN_PROGRESS);
                when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(anyLong())).thenReturn(0);

                apartmentSharingService.createFullPdfForTenant(tenant);

                verify(producer, org.mockito.Mockito.never()).generateFullPdf(anyLong());
            }
        }

        @Nested
        class WhenStatusIsNone {
            @Test
            void shouldTriggerPdfGeneration() {
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.NONE);
                when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(anyLong())).thenReturn(0);

                apartmentSharingService.createFullPdfForTenant(tenant);

                verify(producer).generateFullPdf(apartmentSharing.getId());
            }
        }

        @Nested
        class WhenStatusIsNull {
            @Test
            void shouldTriggerPdfGeneration() {
                apartmentSharing.setDossierPdfDocumentStatus(null);
                when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(anyLong())).thenReturn(0);

                apartmentSharingService.createFullPdfForTenant(tenant);

                assertThat(apartmentSharing.getDossierPdfDocumentStatus()).isEqualTo(FileStatus.NONE);
                verify(producer).generateFullPdf(apartmentSharing.getId());
            }
        }

        @Nested
        class WhenTenantsAreNotValidated {
            @Test
            void shouldNotTriggerPdfGeneration() {
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.NONE);
                when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(anyLong())).thenReturn(1);

                assertThrows(ApartmentSharingUnexpectedException.class, () ->
                        apartmentSharingService.createFullPdfForTenant(tenant)
                );

                verify(producer, Mockito.never()).generateFullPdf(anyLong());
            }
        }

        @Nested
        class WhenDocumentsAreMissing {
            @Test
            void shouldNotTriggerPdfGeneration() {
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.NONE);
                when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(anyLong())).thenReturn(2);

                assertThrows(ApartmentSharingUnexpectedException.class, () ->
                        apartmentSharingService.createFullPdfForTenant(tenant)
                );

                verify(producer, Mockito.never()).generateFullPdf(anyLong());
            }
        }
    }

    @Nested
    class GetFullAnalysisStatus {
        @Test
        void shouldThrowExceptionWhenNoApartmentSharing() {
            tenant.setApartmentSharing(null);
            assertThrows(ApartmentSharingNotFoundException.class, () -> apartmentSharingService.getFullAnalysisStatus(tenant));
        }

        @Test
        void shouldReturnAnalysisStatusForAccessibleDocuments() {
            // Given
            Tenant otherTenant = Tenant.builder().id(2L).keycloakId("k-2").build();
            Guarantor guarantor = Guarantor.builder().id(3L).build();
            Document doc1 = Document.builder().id(10L).build();
            Document doc2 = Document.builder().id(11L).build(); // Other tenant doc
            Document doc3 = Document.builder().id(12L).build(); // Guarantor doc

            tenant.setDocuments(List.of(doc1));
            tenant.setGuarantors(List.of(guarantor));
            guarantor.setDocuments(List.of(doc3)); // doc3 belongs to tenant's guarantor

            otherTenant.setDocuments(List.of(doc2));
            otherTenant.setGuarantors(List.of());

            apartmentSharing.setTenants(List.of(tenant, otherTenant));
            tenant.setKeycloakId("k-1");

            // Mock permissions: Tenant can access himself, but NOT otherTenant
            when(tenantPermissionsService.canAccess("k-1", 1L)).thenReturn(true);
            when(tenantPermissionsService.canAccess("k-1", 2L)).thenReturn(false);

            // Mock document service responses
            DocumentAnalysisStatusResponse status1 = DocumentAnalysisStatusResponse.builder()
                    .documentId(10L)
                    .status(AnalysisStatus.COMPLETED)
                    .build();
            DocumentAnalysisStatusResponse status3 = DocumentAnalysisStatusResponse.builder()
                    .documentId(12L)
                    .status(AnalysisStatus.IN_PROGRESS)
                    .build();

            when(documentService.getDocumentAnalysisStatus(10L, tenant)).thenReturn(status1);
            when(documentService.getDocumentAnalysisStatus(12L, tenant)).thenReturn(status3);

            // When
            var response = apartmentSharingService.getFullAnalysisStatus(tenant);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getListOfDocumentsAnalysisStatus())
                    .hasSize(2)
                    .containsExactlyInAnyOrder(status1, status3); // Should contain doc1 and doc3, but NOT doc2
        }

        @Test
        void shouldExcludeDocumentWhenAccessDenied() {
            // Given
            Document doc1 = Document.builder().id(10L).build();
            tenant.setDocuments(List.of(doc1));
            tenant.setGuarantors(List.of());
            apartmentSharing.setTenants(List.of(tenant));
            tenant.setKeycloakId("k-1");

            when(tenantPermissionsService.canAccess("k-1", 1L)).thenReturn(true);

            // Mock AccessDeniedException for doc1
            when(documentService.getDocumentAnalysisStatus(10L, tenant))
                    .thenThrow(new AccessDeniedException("Denied"));

            // When
            var response = apartmentSharingService.getFullAnalysisStatus(tenant);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getListOfDocumentsAnalysisStatus()).isEmpty();
        }

        @Test
        void shouldExcludeOtherTenantsAndTheirGuarantorDocuments() {
            // Given
            Tenant tenant1 = Tenant.builder().id(1L).keycloakId("k-1").build();
            Guarantor guarantor1 = Guarantor.builder().id(100L).build();
            Document doc1 = Document.builder().id(10L).build(); // tenant1 doc
            Document docGuarantor1 = Document.builder().id(11L).build(); // guarantor1 doc

            tenant1.setDocuments(List.of(doc1));
            tenant1.setGuarantors(List.of(guarantor1));
            guarantor1.setDocuments(List.of(docGuarantor1));

            Tenant tenant2 = Tenant.builder().id(2L).keycloakId("k-2").build();
            Guarantor guarantor2 = Guarantor.builder().id(200L).build();
            Document doc2 = Document.builder().id(20L).build(); // tenant2 doc
            Document docGuarantor2 = Document.builder().id(21L).build(); // guarantor2 doc

            tenant2.setDocuments(List.of(doc2));
            tenant2.setGuarantors(List.of(guarantor2));
            guarantor2.setDocuments(List.of(docGuarantor2));

            apartmentSharing.setTenants(List.of(tenant1, tenant2));
            tenant1.setApartmentSharing(apartmentSharing);

            // Mock permissions: tenant1 can access only themselves
            when(tenantPermissionsService.canAccess("k-1", 1L)).thenReturn(true);
            when(tenantPermissionsService.canAccess("k-1", 2L)).thenReturn(false);

            // Mock document service responses for accessible documents
            DocumentAnalysisStatusResponse status1 = DocumentAnalysisStatusResponse.builder()
                    .documentId(10L).status(AnalysisStatus.COMPLETED).build();
            DocumentAnalysisStatusResponse statusGuarantor1 = DocumentAnalysisStatusResponse.builder()
                    .documentId(11L).status(AnalysisStatus.COMPLETED).build();

            when(documentService.getDocumentAnalysisStatus(10L, tenant1)).thenReturn(status1);
            when(documentService.getDocumentAnalysisStatus(11L, tenant1)).thenReturn(statusGuarantor1);

            // When
            var response = apartmentSharingService.getFullAnalysisStatus(tenant1);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getListOfDocumentsAnalysisStatus())
                    .hasSize(2)
                    .extracting(DocumentAnalysisStatusResponse::getDocumentId)
                    .containsExactlyInAnyOrder(10L, 11L)
                    .doesNotContain(20L, 21L);
        }
    }
}
