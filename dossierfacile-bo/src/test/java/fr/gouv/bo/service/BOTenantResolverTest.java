package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.SharedFileRepository;
import fr.gouv.bo.exception.DocumentNotFoundException;
import fr.gouv.bo.repository.GuarantorRepository;
import fr.gouv.bo.security.BOAccessDenied;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BOTenantResolverTest {

    private static final Long TENANT_ID = 42L;
    private static final Long FILE_ID = 7L;
    private static final Long DOCUMENT_ID = 1L;
    private static final Long GUARANTOR_ID = 3L;

    @Mock
    private SharedFileRepository sharedFileRepository;

    @Mock
    private GuarantorRepository guarantorRepository;

    @Mock
    private DocumentService documentService;

    private BOTenantResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new BOTenantResolver(sharedFileRepository, guarantorRepository, documentService);
    }

    @Nested
    class ResolveTenantFromFileEntity {

        @Test
        void withDocumentAndTenant_returnsTenant() {
            Tenant tenant = tenant(TENANT_ID);
            Document document = Document.builder().id(DOCUMENT_ID).tenant(tenant).build();
            File file = File.builder().id(FILE_ID).document(document).build();

            assertThat(resolver.resolveTenantFromFile(file)).isSameAs(tenant);
        }

        @Test
        void withoutDocument_throwsAccessDenied() {
            File file = File.builder().id(FILE_ID).build();

            assertThatThrownBy(() -> resolver.resolveTenantFromFile(file))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);
        }
    }

    @Nested
    class ResolveTenantFromFileId {

        @Test
        void whenFileExists_returnsTenant() {
            Tenant tenant = tenant(TENANT_ID);
            Document document = Document.builder().id(DOCUMENT_ID).tenant(tenant).build();
            File file = File.builder().id(FILE_ID).document(document).build();
            when(sharedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(file));

            assertThat(resolver.resolveTenantFromFile(FILE_ID)).isSameAs(tenant);
        }

        @Test
        void whenFileNotFound_throwsAccessDenied() {
            when(sharedFileRepository.findById(FILE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resolver.resolveTenantFromFile(FILE_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);
        }
    }

    @Nested
    class ResolveTenantFromGuarantorEntity {

        @Test
        void withTenant_returnsTenant() {
            Tenant tenant = tenant(TENANT_ID);
            Guarantor guarantor = Guarantor.builder().id(GUARANTOR_ID).tenant(tenant).build();

            assertThat(resolver.resolveTenantFromGuarantor(guarantor)).isSameAs(tenant);
        }

        @Test
        void withoutTenant_throwsAccessDenied() {
            Guarantor guarantor = Guarantor.builder().id(GUARANTOR_ID).build();

            assertThatThrownBy(() -> resolver.resolveTenantFromGuarantor(guarantor))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);
        }
    }

    @Nested
    class ResolveTenantFromGuarantorId {

        @Test
        void whenGuarantorExists_returnsTenant() {
            Tenant tenant = tenant(TENANT_ID);
            Guarantor guarantor = Guarantor.builder().id(GUARANTOR_ID).tenant(tenant).build();
            when(guarantorRepository.findById(GUARANTOR_ID)).thenReturn(Optional.of(guarantor));

            assertThat(resolver.resolveTenantFromGuarantor(GUARANTOR_ID)).isSameAs(tenant);
        }

        @Test
        void whenGuarantorNotFound_throwsAccessDenied() {
            when(guarantorRepository.findById(GUARANTOR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resolver.resolveTenantFromGuarantor(GUARANTOR_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);
        }
    }

    @Nested
    class ResolveTenantFromDocumentEntity {

        @Test
        void withDirectTenant_returnsTenant() {
            Tenant tenant = tenant(TENANT_ID);
            Document document = Document.builder().id(DOCUMENT_ID).tenant(tenant).build();

            assertThat(resolver.resolveTenantFromDocument(document)).isSameAs(tenant);
        }

        @Test
        void withGuarantorTenant_returnsTenant() {
            Tenant tenant = tenant(TENANT_ID);
            Guarantor guarantor = Guarantor.builder().id(GUARANTOR_ID).tenant(tenant).build();
            Document document = Document.builder().id(DOCUMENT_ID).guarantor(guarantor).build();

            assertThat(resolver.resolveTenantFromDocument(document)).isSameAs(tenant);
        }

        @Test
        void withoutTenant_throwsAccessDenied() {
            Document document = Document.builder().id(DOCUMENT_ID).build();

            assertThatThrownBy(() -> resolver.resolveTenantFromDocument(document))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);
        }
    }

    @Nested
    class ResolveTenantFromDocumentId {

        @Test
        void whenDocumentExists_returnsTenant() {
            Tenant tenant = tenant(TENANT_ID);
            Document document = Document.builder().id(DOCUMENT_ID).tenant(tenant).build();
            when(documentService.findDocumentById(DOCUMENT_ID)).thenReturn(document);

            assertThat(resolver.resolveTenantFromDocument(DOCUMENT_ID)).isSameAs(tenant);
        }

        @Test
        void whenDocumentNotFound_throwsAccessDenied() {
            when(documentService.findDocumentById(DOCUMENT_ID)).thenThrow(new DocumentNotFoundException(DOCUMENT_ID));

            assertThatThrownBy(() -> resolver.resolveTenantFromDocument(DOCUMENT_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage(BOAccessDenied.GENERIC_MESSAGE);
        }
    }

    private Tenant tenant(Long tenantId) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setId(99L);
        tenant.setApartmentSharing(apartmentSharing);
        return tenant;
    }
}
