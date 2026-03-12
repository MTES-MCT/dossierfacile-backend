package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.FileNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.model.log.EditionType;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    private FileServiceImpl fileService;
    private DocumentServiceImpl documentService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private fr.dossierfacile.common.service.interfaces.LogService logService;

    @Mock
    private fr.dossierfacile.api.front.amqp.Producer producer;

    @Mock
    private DocumentAnalysisReportRepository documentAnalysisReportRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private TenantStatusService tenantStatusService;
    @Mock
    private ApartmentSharingService apartmentSharingService;
    @Mock
    private DocumentHelperService documentHelperService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentServiceImpl(
                documentRepository,
                documentAnalysisReportRepository,
                fileStorageService,
                tenantStatusService,
                apartmentSharingService,
                documentHelperService,
                logService,
                producer
        );
        fileService = new FileServiceImpl(fileRepository, documentService, logService, producer);
    }

    @Nested
    class Delete {

        @Nested
        class WhenGroupTenantTriesToDeleteCoTenantFile {
            @Test
            void shouldThrowFileNotFoundException() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.GROUP);

                Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                Tenant tenant2 = Tenant.builder().id(2L).apartmentSharing(sharing).build();
                sharing.setTenants(List.of(tenant1, tenant2));

                Document document = Document.builder()
                        .id(1L)
                        .tenant(tenant2)
                        .build();
                document.getFiles().clear();

                File file = File.builder()
                        .id(1L)
                        .document(document)
                        .storageFile(StorageFile.builder().build())
                        .build();
                document.getFiles().add(file);

                when(fileRepository.findByIdForTenant(1L, 1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> fileService.delete(1L, tenant1))
                        .isInstanceOf(FileNotFoundException.class);
            }
        }

        @Nested
        class WhenGroupTenantDeletesOwnFile {
            @Test
            void shouldSucceed() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.GROUP);

                Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                Tenant tenant2 = Tenant.builder().id(2L).apartmentSharing(sharing).build();
                sharing.setTenants(List.of(tenant1, tenant2));

                Document document = Document.builder()
                        .id(1L)
                        .tenant(tenant1)
                        .documentCategory(DocumentCategory.RESIDENCY)
                        .build();
                document.getFiles().clear();

                File file = File.builder()
                        .id(1L)
                        .document(document)
                        .storageFile(StorageFile.builder().build())
                        .build();
                document.getFiles().add(file);

                when(fileRepository.findByIdForTenant(1L,  1L)).thenReturn(Optional.of(file));

                assertThatCode(() -> fileService.delete(1L, tenant1)).doesNotThrowAnyException();
                verify(fileRepository).delete(file);
            }
        }

        @Nested
        class WhenCoupleTenantDeletesCoTenantFile {
            @Test
            void shouldSucceed() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.COUPLE);

                Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                Tenant tenant2 = Tenant.builder().id(2L).apartmentSharing(sharing).build();
                sharing.setTenants(List.of(tenant1, tenant2));

                Document document = Document.builder()
                        .id(1L)
                        .tenant(tenant2)
                        .documentCategory(DocumentCategory.RESIDENCY)
                        .build();
                document.getFiles().clear();

                File file = File.builder()
                        .id(1L)
                        .document(document)
                        .storageFile(StorageFile.builder().build())
                        .build();
                document.getFiles().add(file);

                when(fileRepository.findByIdForAppartmentSharing(1L, 1L)).thenReturn(Optional.of(file));

                assertThatCode(() -> fileService.delete(1L, tenant1)).doesNotThrowAnyException();
                verify(fileRepository).delete(file);
            }
        }

        @Nested
        class WhenTenantDeletesGuarantorDocumentFile {
            @Test
            void shouldSucceedAndLogWithResolvedTenant() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.ALONE);

                Tenant tenant = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                Guarantor guarantor = Guarantor.builder().id(10L).tenant(tenant).build();

                Document document = Document.builder()
                        .id(1L)
                        .tenant(null)
                        .guarantor(guarantor)
                        .documentCategory(DocumentCategory.RESIDENCY)
                        .build();
                document.getFiles().clear();
                guarantor.getDocuments().add(document);

                File file = File.builder()
                        .id(1L)
                        .document(document)
                        .storageFile(StorageFile.builder().build())
                        .build();
                document.getFiles().add(file);

                when(fileRepository.findByIdForTenant(1L, 1L)).thenReturn(Optional.of(file));

                assertThatCode(() -> fileService.delete(1L, tenant)).doesNotThrowAnyException();

                verify(fileRepository).delete(file);
                verify(logService).saveDocumentEditedLog(document, tenant, EditionType.DELETE);
            }
        }

        @Nested
        class WhenCoupleTenantDeletesGuarantorDocumentFile {
            @Test
            void shouldSucceedAndLogWithResolvedTenant() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.COUPLE);

                Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                Tenant tenant2 = Tenant.builder().id(2L).apartmentSharing(sharing).build();
                sharing.setTenants(List.of(tenant1, tenant2));

                Guarantor guarantor = Guarantor.builder().id(10L).tenant(tenant1).build();

                Document document = Document.builder()
                        .id(1L)
                        .tenant(null)
                        .guarantor(guarantor)
                        .documentCategory(DocumentCategory.RESIDENCY)
                        .build();
                document.getFiles().clear();
                guarantor.getDocuments().add(document);

                File file = File.builder()
                        .id(1L)
                        .document(document)
                        .storageFile(StorageFile.builder().build())
                        .build();
                document.getFiles().add(file);

                when(fileRepository.findByIdForAppartmentSharing(1L, 1L)).thenReturn(Optional.of(file));

                assertThatCode(() -> fileService.delete(1L, tenant2)).doesNotThrowAnyException();

                verify(fileRepository).delete(file);
                verify(logService).saveDocumentEditedLog(document, tenant1, EditionType.DELETE);
            }
        }
    }
}
