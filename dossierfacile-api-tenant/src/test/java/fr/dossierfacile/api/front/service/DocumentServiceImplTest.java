package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Mock
    private DocumentRepository documentRepository;

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
    class Delete {

        @Nested
        class WhenGroupTenantTriesToDeleteCoTenantDocument {
            @Test
            void shouldThrowAccessDeniedException() {
                ApartmentSharing sharing = new ApartmentSharing();
                sharing.setId(1L);
                sharing.setApplicationType(ApplicationType.GROUP);

                Tenant tenant1 = Tenant.builder().id(1L).apartmentSharing(sharing).build();
                Tenant tenant2 = Tenant.builder().id(2L).apartmentSharing(sharing).build();
                sharing.setTenants(List.of(tenant1, tenant2));

                Document document = Document.builder()
                        .id(1L)
                        .name("doc.pdf")
                        .tenant(tenant2)
                        .build();

                when(documentRepository.findByIdForApartmentSharing(1L, 1L)).thenReturn(Optional.of(document));

                assertThrows(AccessDeniedException.class, () ->
                        documentService.delete(1L, tenant1)
                );
            }
        }

        @Nested
        class WhenGroupTenantDeletesOwnDocument {
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
                        .name("doc.pdf")
                        .documentCategory(DocumentCategory.IDENTIFICATION)
                        .tenant(tenant1)
                        .build();
                tenant1.getDocuments().add(document);

                when(documentRepository.findByIdForApartmentSharing(1L, 1L)).thenReturn(Optional.of(document));

                assertDoesNotThrow(() -> documentService.delete(1L, tenant1));
                verify(documentRepository).delete(document);
            }
        }

        @Nested
        class WhenCoupleTenantDeletesCoTenantDocument {
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
                        .name("doc.pdf")
                        .documentCategory(DocumentCategory.IDENTIFICATION)
                        .tenant(tenant2)
                        .build();
                tenant2.getDocuments().add(document);

                when(documentRepository.findByIdForApartmentSharing(1L, 1L)).thenReturn(Optional.of(document));

                assertDoesNotThrow(() -> documentService.delete(1L, tenant1));
                verify(documentRepository).delete(document);
            }
        }
    }
}
