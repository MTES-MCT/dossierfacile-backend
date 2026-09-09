package fr.dossierfacile.api.front.register;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.register.form.IDocumentResidencyForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@AllArgsConstructor
public abstract class AbstractDocumentResidencySaveStep<T extends DocumentForm & IDocumentResidencyForm> extends AbstractDocumentSaveStep<T> {

    protected final DocumentHelperService documentHelperService;
    protected final TenantCommonRepository tenantRepository;
    protected final DocumentRepository documentRepository;
    protected final TenantStatusService tenantStatusService;
    protected final ApartmentSharingService apartmentSharingService;

    protected DocumentSaveResult processResidencyDocument(Tenant tenant, Document document, T documentResidencyForm) {
        DocumentSubCategory documentSubCategory = documentResidencyForm.getTypeDocumentResidency();
        boolean targetNoDocument = (documentSubCategory == DocumentSubCategory.OTHER_RESIDENCY);
        boolean created = document.getId() == null;
        boolean isStayingNoDocument = !created
                && Boolean.TRUE.equals(document.getNoDocument())
                && targetNoDocument;

        boolean hasResidencyInfoChanged = documentSubCategory != document.getDocumentSubCategory()
                || documentResidencyForm.getCategoryStep() != document.getDocumentCategoryStep()
                || !Objects.equals(document.getCustomText(), documentResidencyForm.getCustomText());

        boolean edited = !isStayingNoDocument || hasResidencyInfoChanged;

        if (!isStayingNoDocument) {
            document.setDocumentStatus(DocumentStatus.TO_PROCESS);
            document.setDocumentDeniedReasons(null);
        }

        document.setDocumentSubCategory(documentSubCategory);
        document.setDocumentCategoryStep(documentResidencyForm.getCategoryStep());

        if (Boolean.FALSE.equals(document.getNoDocument()) && targetNoDocument) {
            deleteFilesIfExistedBefore(document);
        }

        if (targetNoDocument) {
            document.setCustomText(documentResidencyForm.getCustomText());
            document.setNoDocument(true);
        } else {
            document.setCustomText(null);
            document.setNoDocument(false);
        }
        documentRepository.save(document);

        if (!targetNoDocument) {
            if (documentResidencyForm.getDocuments() != null && !documentResidencyForm.getDocuments().isEmpty()) {
                saveFiles(documentResidencyForm, document);
            } else {
                log.info("Refreshing info in [RESIDENCY] document with ID [" + document.getId() + "]");
            }
        }

        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.RESIDENCY);
        if (edited) {
            apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        }
        tenantStatusService.updateTenantStatus(tenant);
        tenantRepository.save(tenant);
        return new DocumentSaveResult(document, created, edited);
    }

    private void deleteFilesIfExistedBefore(Document document) {
        documentHelperService.deleteFiles(document);
    }
}
