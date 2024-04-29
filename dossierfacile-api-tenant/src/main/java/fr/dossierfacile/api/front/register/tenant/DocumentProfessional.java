package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.register.AbstractDocumentSaveStep;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.DocumentProfessionalForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class DocumentProfessional extends AbstractDocumentSaveStep<DocumentProfessionalForm>
        implements SaveStep<DocumentProfessionalForm> {

    private final TenantCommonRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final TenantStatusService tenantStatusService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    protected Document saveDocument(Tenant tenant, DocumentProfessionalForm documentProfessionalForm) {
        DocumentSubCategory documentSubCategory = documentProfessionalForm.getTypeDocumentProfessional();
        Document document = documentRepository.findFirstByDocumentCategoryAndTenant(DocumentCategory.PROFESSIONAL, tenant)
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.PROFESSIONAL)
                        .tenant(tenant)
                        .build());
        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
        document.setDocumentDeniedReasons(null);
        document.setDocumentSubCategory(documentSubCategory);
        documentRepository.save(document);

        saveFiles(documentProfessionalForm, document);

        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.PROFESSIONAL);
        documentService.resetValidatedOrInProgressDocumentsAccordingCategories(tenant.getDocuments(), List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.FINANCIAL, DocumentCategory.TAX));

        tenantStatusService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenantRepository.save(tenant);
        return document;
    }
}
