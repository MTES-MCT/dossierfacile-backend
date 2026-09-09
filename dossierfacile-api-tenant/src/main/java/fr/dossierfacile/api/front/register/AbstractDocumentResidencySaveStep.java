package fr.dossierfacile.api.front.register;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.register.form.IDocumentResidencyForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
        boolean created = document.getId() == null;

        boolean newFilesPresent = hasNewFiles(documentResidencyForm);
        boolean existingFilesPresent = hasExistingFiles(created, document);

        boolean isOtherResidency = (documentSubCategory == DocumentSubCategory.OTHER_RESIDENCY);
        boolean targetNoDocument = computeTargetNoDocument(documentSubCategory, newFilesPresent, existingFilesPresent, document.getDocumentSubCategory());

        // Indique si le document était déjà sans fichier et le reste après la sauvegarde
        boolean isStayingNoDocument = !created
                && Boolean.TRUE.equals(document.getNoDocument())
                && targetNoDocument;

        // Détecte si des informations du formulaire ont changé (sous-catégorie, étape ou texte d'explication)
        boolean infoChanged = hasResidencyInfoChanged(document, documentSubCategory, documentResidencyForm.getCategoryStep(), documentResidencyForm.getCustomText());

        // Si le document reste sans fichier ET qu'aucune info n'a changé, pas besoin de réinitialiser le statut
        boolean isUnchangedNoDocument = isStayingNoDocument && !infoChanged;
        boolean edited = !isUnchangedNoDocument;

        if (!isUnchangedNoDocument) {
            document.setDocumentStatus(DocumentStatus.TO_PROCESS);
            document.setDocumentDeniedReasons(null);
        }

        document.setDocumentSubCategory(documentSubCategory);
        document.setDocumentCategoryStep(documentResidencyForm.getCategoryStep());

        if (Boolean.FALSE.equals(document.getNoDocument()) && targetNoDocument) {
            deleteFilesIfExistedBefore(document);
        }

        document.setNoDocument(targetNoDocument);
        document.setCustomText(isOtherResidency ? documentResidencyForm.getCustomText() : null);
        documentRepository.save(document);

        saveFilesOrLogInfo(documentResidencyForm, document);

        tenant.lastUpdateDateProfile(LocalDateTime.now(ZoneId.systemDefault()), DocumentCategory.RESIDENCY);
        if (edited) {
            apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        }
        tenantStatusService.updateTenantStatus(tenant);
        tenantRepository.save(tenant);
        return new DocumentSaveResult(document, created, edited);
    }

    private boolean hasNewFiles(IDocumentResidencyForm form) {
        return form.getDocuments() != null
                && form.getDocuments().stream().anyMatch(f -> f != null && !f.isEmpty());
    }

    private boolean hasExistingFiles(boolean created, Document document) {
        return !created
                && Boolean.FALSE.equals(document.getNoDocument())
                && document.getFiles() != null
                && !document.getFiles().isEmpty();
    }

    private boolean computeTargetNoDocument(DocumentSubCategory subCategory, boolean hasNewFiles, boolean hasExistingFiles, DocumentSubCategory previousSubCategory) {
        if (subCategory != DocumentSubCategory.OTHER_RESIDENCY) {
            return false;
        }
        return !hasNewFiles && (!hasExistingFiles || previousSubCategory != DocumentSubCategory.OTHER_RESIDENCY);
    }

    private boolean hasResidencyInfoChanged(Document document, DocumentSubCategory subCategory, DocumentCategoryStep step, String customText) {
        return subCategory != document.getDocumentSubCategory()
                || step != document.getDocumentCategoryStep()
                || !Objects.equals(document.getCustomText(), customText);
    }

    private void saveFilesOrLogInfo(T form, Document document) {
        if (form.getDocuments() != null && !form.getDocuments().isEmpty()) {
            saveFiles(form, document);
        } else {
            log.info("Refreshing info in [RESIDENCY] document with ID [" + document.getId() + "]");
        }
    }

    private void deleteFilesIfExistedBefore(Document document) {
        documentHelperService.deleteFiles(document);
    }
}

