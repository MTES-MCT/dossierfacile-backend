package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.register.AbstractDocumentResidencySaveStep;
import fr.dossierfacile.api.front.register.DocumentSaveResult;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.DocumentResidencyForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
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
public class DocumentResidency extends AbstractDocumentResidencySaveStep<DocumentResidencyForm> implements SaveStep<DocumentResidencyForm> {

    public DocumentResidency(
            DocumentHelperService documentHelperService,
            TenantCommonRepository tenantRepository,
            DocumentRepository documentRepository,
            TenantStatusService tenantStatusService,
            ApartmentSharingService apartmentSharingService) {
        super(documentHelperService, tenantRepository, documentRepository, tenantStatusService, apartmentSharingService);
    }

    @Override
    protected DocumentSaveResult saveDocument(Tenant tenant, DocumentResidencyForm documentResidencyForm) {
        Document document = documentRepository.findFirstByDocumentCategoryAndTenant(DocumentCategory.RESIDENCY, tenant)
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.RESIDENCY)
                        .tenant(tenant)
                        .build());
        return processResidencyDocument(tenant, document, documentResidencyForm);
    }
}
