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
import java.time.ZoneId;
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

        // Indique si le document était déjà sans justificatif de revenu (ex: NO_INCOME) et le demeure
        boolean isStayingNoDocument = !created
                && Boolean.TRUE.equals(document.getNoDocument())
                && Boolean.TRUE.equals(documentFinancialForm.getNoDocument());

        int effectiveMonthlySum = computeEffectiveMonthlySum(documentFinancialForm);

        boolean infoChanged = hasFinancialInfoChanged(
                document,
                documentSubCategory,
                documentFinancialForm.getCategoryStep(),
                effectiveMonthlySum,
                documentFinancialForm.getCustomText()
        );

        // Si le document reste sans justificatif ET qu'aucune info financière n'a changé, pas de réinitialisation du statut
        boolean isUnchangedNoDocument = isStayingNoDocument && !infoChanged;
        boolean edited = !isUnchangedNoDocument;
        boolean needToBeReValidated = false;

        if (!isUnchangedNoDocument) {
            document.setDocumentStatus(DocumentStatus.TO_PROCESS);
            document.setDocumentDeniedReasons(null);
            if (!isStayingNoDocument) {
                document.setCustomText(null);
            }
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

        updateFilesAndCustomText(documentFinancialForm, document);
        documentRepository.save(document);

        tenant.lastUpdateDateProfile(LocalDateTime.now(ZoneId.systemDefault()), DocumentCategory.FINANCIAL);
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

    private int computeEffectiveMonthlySum(IDocumentFinancialForm form) {
        Integer sum = form.getMonthlySum();
        if (sum != null && sum > 0 && form.getTypeDocumentFinancial() != DocumentSubCategory.NO_INCOME) {
            return sum;
        }
        return 0;
    }

    private boolean hasFinancialInfoChanged(Document document, DocumentSubCategory subCategory, fr.dossierfacile.common.enums.DocumentCategoryStep step, int monthlySum, String customText) {
        return subCategory != document.getDocumentSubCategory()
                || step != document.getDocumentCategoryStep()
                || !Objects.equals(document.getMonthlySum(), monthlySum)
                || !Objects.equals(document.getCustomText(), customText);
    }

    private void updateFilesAndCustomText(T documentFinancialForm, Document document) {
        if (Boolean.TRUE.equals(documentFinancialForm.getNoDocument())) {
            document.setCustomText(documentFinancialForm.getCustomText());
            return;
        }

        if (!documentFinancialForm.getDocuments().isEmpty()) {
            saveFiles(documentFinancialForm, document);
            document.setCustomText(null);
        } else {
            log.info("Refreshing info in [FINANCIAL] document with ID [" + document.getId() + "]");
        }
    }

    private void deleteFilesIfExistedBefore(Document document) {
        documentHelperService.deleteFiles(document);
    }
}
