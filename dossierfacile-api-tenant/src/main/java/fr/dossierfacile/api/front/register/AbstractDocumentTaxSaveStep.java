package fr.dossierfacile.api.front.register;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.register.form.IDocumentTaxForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
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
import java.util.List;
import java.util.Objects;

import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_NAME;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_TAX;

@Slf4j
@AllArgsConstructor
public abstract class AbstractDocumentTaxSaveStep<T extends DocumentForm & IDocumentTaxForm> extends AbstractDocumentSaveStep<T> {

    protected final DocumentHelperService documentHelperService;
    protected final TenantCommonRepository tenantRepository;
    protected final DocumentRepository documentRepository;
    protected final DocumentService documentService;
    protected final TenantStatusService tenantStatusService;
    protected final ApartmentSharingService apartmentSharingService;

    protected DocumentSaveResult processTaxDocument(Tenant tenant, Document document, T documentTaxForm, List<Document> documentsToReset) {
        DocumentSubCategory documentSubCategory = documentTaxForm.getTypeDocumentTax();
        boolean created = document.getId() == null;
        boolean isStayingNoDocument = !created
                && Boolean.TRUE.equals(document.getNoDocument())
                && Boolean.TRUE.equals(documentTaxForm.getNoDocument());

        boolean hasTaxInfoChanged = documentSubCategory != document.getDocumentSubCategory()
                || documentTaxForm.getCategoryStep() != document.getDocumentCategoryStep()
                || !Objects.equals(documentTaxForm.getCustomText(), document.getCustomText());

        boolean edited = !isStayingNoDocument || hasTaxInfoChanged;
        boolean needToBeReValidated = false;

        if (!isStayingNoDocument) {
            document.setDocumentStatus(DocumentStatus.TO_PROCESS);
            document.setDocumentDeniedReasons(null);
            document.setCustomText(null);
            needToBeReValidated = true;
        }

        document.setDocumentSubCategory(documentSubCategory);
        document.setDocumentCategoryStep(documentTaxForm.getCategoryStep());

        if (Boolean.FALSE.equals(document.getNoDocument()) && Boolean.TRUE.equals(documentTaxForm.getNoDocument())) {
            deleteFilesIfExistedBefore(document);
        }

        document.setNoDocument(documentTaxForm.getNoDocument());
        if (documentTaxForm.getAvisDetected() != null) {
            document.setAvisDetected(documentTaxForm.getAvisDetected());
        }
        if (documentSubCategory == OTHER_TAX) {
            document.setCustomText(documentTaxForm.getCustomText());
        }
        documentRepository.save(document);

        if (documentSubCategory == MY_NAME
                || (documentSubCategory == OTHER_TAX && !documentTaxForm.getNoDocument())) {
            if (documentTaxForm.getDocuments().size() > 0) {
                saveFiles(documentTaxForm, document);
            } else {
                log.info("Refreshing info in [TAX] document with ID [" + document.getId() + "]");
            }
        }

        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.TAX);
        if (needToBeReValidated) {
            documentService.resetValidatedOrInProgressDocumentsAccordingCategories(documentsToReset, List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.FINANCIAL, DocumentCategory.TAX));
        }

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
