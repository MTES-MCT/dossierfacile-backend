package fr.dossierfacile.common.service;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.DocumentCommonRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.ApartmentSharingCommonService;
import fr.dossierfacile.common.service.interfaces.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DocumentDeletionCommonServiceImplTest {

    private DocumentCommonRepository documentRepository;
    private TenantCommonRepository tenantRepository;
    private LogService logService;
    private ApartmentSharingCommonService apartmentSharingCommonService;

    private DocumentDeletionCommonServiceImpl service;

    private ApartmentSharing apartmentSharing;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentCommonRepository.class);
        tenantRepository = mock(TenantCommonRepository.class);
        logService = mock(LogService.class);
        apartmentSharingCommonService = mock(ApartmentSharingCommonService.class);
        service = new DocumentDeletionCommonServiceImpl(documentRepository, tenantRepository, logService, apartmentSharingCommonService);

        apartmentSharing = new ApartmentSharing();
        tenant = Tenant.builder().id(1L).readyForAutoValidation(true).apartmentSharing(apartmentSharing).documents(new ArrayList<>()).build();
    }

    private Document tenantDocument(long id) {
        Document document = Document.builder().id(id).tenant(tenant).build();
        tenant.getDocuments().add(document);
        return document;
    }

    @Test
    void deletesATenantDocumentWithTheSharedInvariants() {
        Document document = tenantDocument(10L);
        Document sibling = tenantDocument(11L);

        Tenant result = service.deleteDocument(document, null, null);

        assertThat(result).isSameAs(tenant);
        assertThat(tenant.getDocuments()).containsExactly(sibling);
        assertThat(tenant.getReadyForAutoValidation()).isFalse();
        verify(logService).saveDocumentDeletedLog(document, tenant, null, null);
        verify(documentRepository).delete(document);
        verify(tenantRepository).save(tenant);
        verify(apartmentSharingCommonService).resetDossierPdfGenerated(apartmentSharing);
    }
}
