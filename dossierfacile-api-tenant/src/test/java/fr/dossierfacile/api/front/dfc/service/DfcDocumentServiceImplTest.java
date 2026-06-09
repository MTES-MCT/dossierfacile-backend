package fr.dossierfacile.api.front.dfc.service;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.exception.TenantNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.TenantPermissionsService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DfcDocumentServiceImplTest {

    private static final String TOKEN = "doc-token-123";
    private static final Long TENANT_ID = 1L;
    private static final Long APARTMENT_SHARING_ID = 77L;
    private static final String CLIENT_ID = "partner-client";

    @Mock
    private TenantService tenantService;
    @Mock
    private TenantPermissionsService tenantPermissionsService;
    @Mock
    private DocumentRepository documentRepository;
    @InjectMocks
    private DfcDocumentServiceImpl service;

    private Tenant tenantWithApartmentSharing() {
        ApartmentSharing apartmentSharing = ApartmentSharing.builder().id(APARTMENT_SHARING_ID).build();
        return Tenant.builder().id(TENANT_ID).apartmentSharing(apartmentSharing).build();
    }

    private Document documentWithWatermark() {
        return Document.builder().id(10L).name(TOKEN).watermarkFile(new StorageFile()).build();
    }

    @Test
    void shouldThrowTenantNotFoundWhenTenantIsNull() {
        when(tenantService.findById(TENANT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.getAuthorizedDocument(TOKEN, TENANT_ID, CLIENT_ID))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void shouldThrowTenantNotFoundWhenApartmentSharingIsNull() {
        when(tenantService.findById(TENANT_ID)).thenReturn(Tenant.builder().id(TENANT_ID).build());

        assertThatThrownBy(() -> service.getAuthorizedDocument(TOKEN, TENANT_ID, CLIENT_ID))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void shouldThrowAccessDeniedWhenPartnerHasNoConsent() {
        when(tenantService.findById(TENANT_ID)).thenReturn(tenantWithApartmentSharing());
        when(tenantPermissionsService.clientCanAccess(CLIENT_ID, TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getAuthorizedDocument(TOKEN, TENANT_ID, CLIENT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldThrowDocumentNotFoundWhenDocumentNotInApartmentSharing() {
        when(tenantService.findById(TENANT_ID)).thenReturn(tenantWithApartmentSharing());
        when(tenantPermissionsService.clientCanAccess(CLIENT_ID, TENANT_ID)).thenReturn(true);
        when(documentRepository.findByNameForApartmentSharing(TOKEN, APARTMENT_SHARING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAuthorizedDocument(TOKEN, TENANT_ID, CLIENT_ID))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void shouldThrowDocumentNotFoundWhenNoWatermark() {
        Document withoutWatermark = Document.builder().id(10L).name(TOKEN).build();
        when(tenantService.findById(TENANT_ID)).thenReturn(tenantWithApartmentSharing());
        when(tenantPermissionsService.clientCanAccess(CLIENT_ID, TENANT_ID)).thenReturn(true);
        when(documentRepository.findByNameForApartmentSharing(TOKEN, APARTMENT_SHARING_ID))
                .thenReturn(Optional.of(withoutWatermark));

        assertThatThrownBy(() -> service.getAuthorizedDocument(TOKEN, TENANT_ID, CLIENT_ID))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void shouldReturnDocumentWhenAuthorizedAndWatermarked() {
        Document document = documentWithWatermark();
        when(tenantService.findById(TENANT_ID)).thenReturn(tenantWithApartmentSharing());
        when(tenantPermissionsService.clientCanAccess(CLIENT_ID, TENANT_ID)).thenReturn(true);
        when(documentRepository.findByNameForApartmentSharing(TOKEN, APARTMENT_SHARING_ID))
                .thenReturn(Optional.of(document));

        Document result = service.getAuthorizedDocument(TOKEN, TENANT_ID, CLIENT_ID);

        assertThat(result).isSameAs(document);
    }
}
