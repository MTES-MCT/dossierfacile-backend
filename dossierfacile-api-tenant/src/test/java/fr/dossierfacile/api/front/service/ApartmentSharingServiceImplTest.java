package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.ApartmentSharingNotFoundException;
import fr.dossierfacile.api.front.model.tenant.FullFolderFile;
import fr.dossierfacile.api.front.repository.ApiTenantLogRepository;
import fr.dossierfacile.common.entity.ApartmentSharing;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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

    @MockBean
    private ApartmentSharingRepository apartmentSharingRepository;
    @MockBean
    private ApartmentSharingLinkRepository apartmentSharingLinkRepository;
    @MockBean
    private TenantCommonRepository tenantRepository;
    @MockBean
    private ApplicationFullMapper applicationFullMapper;
    @MockBean
    private ApplicationLightMapper applicationLightMapper;
    @MockBean
    private ApplicationBasicMapper applicationBasicMapper;
    @MockBean
    private FileStorageService fileStorageService;
    @MockBean
    private LinkLogService linkLogService;
    @MockBean
    private Producer producer;
    @MockBean
    private ApartmentSharingCommonService apartmentSharingCommonService;
    @MockBean
    private ApiTenantLogRepository tenantLogRepository;
    @MockBean
    private LogService logService;
    @MockBean
    private fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService bruteForceProtectionService;

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
}
