package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.register.AbstractDocumentTaxSaveStep;
import fr.dossierfacile.api.front.register.DocumentSaveResult;
import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DocumentTax extends AbstractDocumentTaxSaveStep<DocumentTaxForm> {

    public DocumentTax(
            DocumentHelperService documentHelperService,
            TenantCommonRepository tenantRepository,
            DocumentRepository documentRepository,
            DocumentService documentService,
            TenantStatusService tenantStatusService,
            ApartmentSharingService apartmentSharingService) {
        super(documentHelperService, tenantRepository, documentRepository, documentService, tenantStatusService, apartmentSharingService);
    }

    @Override
    protected DocumentSaveResult saveDocument(Tenant tenant, DocumentTaxForm documentTaxForm) {
        Document document = documentRepository.findFirstByDocumentCategoryAndTenant(DocumentCategory.TAX, tenant)
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.TAX)
                        .tenant(tenant)
                        .build());
        return processTaxDocument(tenant, document, documentTaxForm, tenant.getDocuments());
    }
}
