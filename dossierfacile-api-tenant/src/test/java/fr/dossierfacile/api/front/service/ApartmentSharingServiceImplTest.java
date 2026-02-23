package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.model.tenant.FullFolderFile;
import fr.dossierfacile.api.front.repository.ApiTenantLogRepository;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.ApartmentSharingLink;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    private fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService bruteForceProtectionService;
    @MockitoBean
    private DocumentRepository documentRepository;

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

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
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

                assertThrows(fr.dossierfacile.api.front.exception.ApartmentSharingUnexpectedException.class, () ->
                        apartmentSharingService.createFullPdfForTenant(tenant)
                );

                verify(producer, org.mockito.Mockito.never()).generateFullPdf(anyLong());
            }
        }

        @Nested
        class WhenDocumentsAreMissing {
            @Test
            void shouldNotTriggerPdfGeneration() {
                apartmentSharing.setDossierPdfDocumentStatus(FileStatus.NONE);
                when(tenantRepository.countTenantsInTheApartmentNotValidatedOrWithSomeNullDocument(anyLong())).thenReturn(2);

                assertThrows(fr.dossierfacile.api.front.exception.ApartmentSharingUnexpectedException.class, () ->
                        apartmentSharingService.createFullPdfForTenant(tenant)
                );

                verify(producer, org.mockito.Mockito.never()).generateFullPdf(anyLong());
            }
        }
    }

    @Nested
    class FindDocumentByLink {

        private final UUID token = UUID.randomUUID();
        private final String documentName = "test-doc.pdf";

        @Nested
        class WhenLinkIsValidAndDocumentExists {
            @Test
            void shouldReturnDocument() {
                ApartmentSharingLink link = new ApartmentSharingLink();
                link.setApartmentSharing(apartmentSharing);

                Document document = Document.builder()
                        .id(1L)
                        .name(documentName)
                        .build();

                when(apartmentSharingLinkRepository.findValidLinkByToken(token, true))
                        .thenReturn(Optional.of(link));
                when(documentRepository.findByNameForApartmentSharing(documentName, apartmentSharing.getId()))
                        .thenReturn(Optional.of(document));

                Document result = apartmentSharingService.findDocumentByLink(token, documentName);

                assertThat(result).isNotNull();
                assertThat(result.getName()).isEqualTo(documentName);
            }
        }

        @Nested
        class WhenLinkIsInvalid {
            @Test
            void shouldThrowApartmentSharingNotFoundException() {
                when(apartmentSharingLinkRepository.findValidLinkByToken(token, true))
                        .thenReturn(Optional.empty());

                assertThrows(ApartmentSharingNotFoundException.class, () ->
                        apartmentSharingService.findDocumentByLink(token, documentName)
                );
            }
        }

       @Nested
        class WhenDocumentNotInSharing {
            @Test
            void shouldThrowApartmentSharingNotFoundException() {
                ApartmentSharingLink link = new ApartmentSharingLink();
                link.setApartmentSharing(apartmentSharing);

                when(apartmentSharingLinkRepository.findValidLinkByToken(token, true))
                        .thenReturn(Optional.of(link));
                when(documentRepository.findByNameForApartmentSharing(documentName, apartmentSharing.getId()))
                        .thenReturn(Optional.empty());

                assertThrows(ApartmentSharingNotFoundException.class, () ->
                        apartmentSharingService.findDocumentByLink(token, documentName)
                );
            }
        }
    }
}
