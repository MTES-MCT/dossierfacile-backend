package fr.dossierfacile.api.front.register;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.register.form.IDocumentFinancialForm;
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

@Slf4j
@AllArgsConstructor
public abstract class AbstractDocumentFinancialSaveStep<T extends DocumentForm & IDocumentFinancialForm> extends AbstractDocumentSaveStep<T> {

    protected final DocumentHelperService documentHelperService;
    protected final TenantCommonRepository tenantRepository;
    protected final DocumentRepository documentRepository;
    protected final DocumentService documentService;
    protected final TenantStatusService tenantStatusService;
    protected final ApartmentSharingService apartmentSharingService;

    protected DocumentSaveResult processFinancialDocument(Tenant tenant, Document document, T documentFinancialForm, List<Document> documentsToReset) {
        DocumentSubCategory documentSubCategory = documentFinancialForm.getTypeDocumentFinancial();
        boolean created = document.getId() == null;
        boolean isStayingNoDocument = !created
                && Boolean.TRUE.equals(document.getNoDocument())
                && Boolean.TRUE.equals(documentFinancialForm.getNoDocument());

        int effectiveMonthlySum = (documentFinancialForm.getMonthlySum() != null && documentFinancialForm.getMonthlySum() > 0
                && documentFinancialForm.getTypeDocumentFinancial() != DocumentSubCategory.NO_INCOME)
                ? documentFinancialForm.getMonthlySum() : 0;

        boolean hasFinancialInfoChanged = documentSubCategory != document.getDocumentSubCategory()
                || documentFinancialForm.getCategoryStep() != document.getDocumentCategoryStep()
                || !Objects.equals(document.getMonthlySum(), effectiveMonthlySum)
                || !Objects.equals(document.getCustomText(), documentFinancialForm.getCustomText());

        boolean edited = !isStayingNoDocument || hasFinancialInfoChanged;
        boolean needToBeReValidated = false;

        if (!isStayingNoDocument) {
            document.setDocumentStatus(DocumentStatus.TO_PROCESS);
            document.setDocumentDeniedReasons(null);
            document.setCustomText(null);
            needToBeReValidated = true;
        }

        document.setDocumentSubCategory(documentSubCategory);
        document.setDocumentCategoryStep(documentFinancialForm.getCategoryStep());
        document.setMonthlySum(effectiveMonthlySum);

        if (Boolean.FALSE.equals(document.getNoDocument()) && Boolean.TRUE.equals(documentFinancialForm.getNoDocument())) {
            deleteFilesIfExistedBefore(document);
        }

        document.setNoDocument(documentFinancialForm.getNoDocument());
        documentRepository.save(document);

        if (Boolean.FALSE.equals(documentFinancialForm.getNoDocument())) {
            if (!documentFinancialForm.getDocuments().isEmpty()) {
                saveFiles(documentFinancialForm, document);
                document.setCustomText(null);
            } else {
                log.info("Refreshing info in [FINANCIAL] document with ID [" + document.getId() + "]");
            }
        } else {
            document.setCustomText(documentFinancialForm.getCustomText());
        }
        documentRepository.save(document);

        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.FINANCIAL);
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
