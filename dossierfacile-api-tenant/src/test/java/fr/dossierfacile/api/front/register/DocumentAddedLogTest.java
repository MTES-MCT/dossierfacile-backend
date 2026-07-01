package fr.dossierfacile.api.front.register;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.register.tenant.DocumentTax;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.security.interfaces.ClientAuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.FileUploadPreprocessor;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.common.service.interfaces.PartnerCallBackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ensures DOCUMENT_ADDED is logged only when a document is created, not when a file
 * is added to a pre-existing document (which is tracked by FILE_ADDED instead).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DocumentTax.class)
class DocumentAddedLogTest {

    @Autowired
    DocumentTax documentTax;

    @MockitoBean
    DocumentHelperService documentHelperService;
    @MockitoBean
    TenantCommonRepository tenantCommonRepository;
    @MockitoBean
    DocumentRepository documentRepository;
    @MockitoBean
    DocumentService documentService;
    @MockitoBean
    TenantStatusService tenantStatusService;
    @MockitoBean
    ApartmentSharingService apartmentSharingService;
    @MockitoBean
    TenantMapper tenantMapper;
    @MockitoBean
    PartnerCallBackService partnerCallBackService;
    @MockitoBean
    LogService logService;
    @MockitoBean
    Producer producer;
    @MockitoBean
    ClientAuthenticationFacade clientAuthenticationFacade;
    @MockitoBean
    FileUploadPreprocessor fileUploadPreprocessor;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        ApartmentSharing apartmentSharing = ApartmentSharing.builder().id(1L).applicationType(ApplicationType.ALONE).build();
        tenant = Tenant.builder().id(1L).apartmentSharing(apartmentSharing).documents(new ArrayList<>()).build();
        apartmentSharing.setTenants(new ArrayList<>());
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private DocumentTaxForm taxFormWithoutFile() {
        DocumentTaxForm form = new DocumentTaxForm();
        form.setTypeDocumentTax(DocumentSubCategory.MY_NAME);
        form.setNoDocument(false);
        form.setDocuments(Collections.emptyList());
        return form;
    }

    @Test
    void should_log_document_added_when_document_is_created() {
        when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.TAX), any()))
                .thenReturn(Optional.empty());

        documentTax.saveStep(tenant, taxFormWithoutFile());

        verify(logService).saveDocumentAddedLog(any(Document.class), eq(tenant));
    }

    @Test
    void should_not_log_document_added_when_updating_existing_document() {
        Document existingDocument = Document.builder()
                .id(200L)
                .documentCategory(DocumentCategory.TAX)
                .tenant(tenant)
                .build();
        tenant.setDocuments(new ArrayList<>(List.of(existingDocument)));
        when(documentRepository.findFirstByDocumentCategoryAndTenant(eq(DocumentCategory.TAX), any()))
                .thenReturn(Optional.of(existingDocument));

        documentTax.saveStep(tenant, taxFormWithoutFile());

        verify(logService, never()).saveDocumentAddedLog(any(), any());
    }

}
