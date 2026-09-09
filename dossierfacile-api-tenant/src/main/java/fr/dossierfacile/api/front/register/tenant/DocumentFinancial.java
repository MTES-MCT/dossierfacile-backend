package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.register.AbstractDocumentFinancialSaveStep;
import fr.dossierfacile.api.front.register.DocumentSaveResult;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
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
public class DocumentFinancial extends AbstractDocumentFinancialSaveStep<DocumentFinancialForm> implements SaveStep<DocumentFinancialForm> {

    public DocumentFinancial(
            DocumentHelperService documentHelperService,
            TenantCommonRepository tenantRepository,
            DocumentRepository documentRepository,
            DocumentService documentService,
            TenantStatusService tenantStatusService,
            ApartmentSharingService apartmentSharingService) {
        super(documentHelperService, tenantRepository, documentRepository, documentService, tenantStatusService, apartmentSharingService);
    }

    @Override
    protected DocumentSaveResult saveDocument(Tenant tenant, DocumentFinancialForm documentFinancialForm) {
        Document document = documentRepository.findByDocumentCategoryAndTenantAndId(DocumentCategory.FINANCIAL, tenant, documentFinancialForm.getId())
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.FINANCIAL)
                        .documentCategoryStep(documentFinancialForm.getCategoryStep())
                        .tenant(tenant)
                        .build());
        return processFinancialDocument(tenant, document, documentFinancialForm, tenant.getDocuments());
    }
}
