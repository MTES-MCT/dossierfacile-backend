package fr.dossierfacile.api.front.register.guarantor.natural_person;

import fr.dossierfacile.api.front.exception.GuarantorNotFoundException;
import fr.dossierfacile.api.front.register.AbstractDocumentSaveStep;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentFinancialGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.GuarantorRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TypeGuarantor;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class DocumentFinancialGuarantorNaturalPerson extends AbstractDocumentSaveStep<DocumentFinancialGuarantorNaturalPersonForm> implements SaveStep<DocumentFinancialGuarantorNaturalPersonForm> {

    private final DocumentHelperService documentHelperService;
    private final TenantCommonRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final GuarantorRepository guarantorRepository;
    private final DocumentService documentService;
    private final TenantStatusService tenantStatusService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    protected Document saveDocument(Tenant tenant, DocumentFinancialGuarantorNaturalPersonForm documentFinancialGuarantorNaturalPersonForm) {
        Guarantor guarantor = guarantorRepository.findByTenantAndTypeGuarantorAndId(tenant, TypeGuarantor.NATURAL_PERSON, documentFinancialGuarantorNaturalPersonForm.getGuarantorId())
                .orElseThrow(() -> new GuarantorNotFoundException(documentFinancialGuarantorNaturalPersonForm.getGuarantorId()));

        DocumentSubCategory documentSubCategory = documentFinancialGuarantorNaturalPersonForm.getTypeDocumentFinancial();
        Document document = documentRepository.findByDocumentCategoryAndGuarantorAndId(DocumentCategory.FINANCIAL, guarantor, documentFinancialGuarantorNaturalPersonForm.getDocumentId())
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.FINANCIAL)
                        .guarantor(guarantor)
                        .build());
        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
        document.setDocumentDeniedReasons(null);
        document.setDocumentSubCategory(documentSubCategory);
        document.setMonthlySum(documentFinancialGuarantorNaturalPersonForm.getMonthlySum());
        if (document.getNoDocument() != null && !document.getNoDocument() && documentFinancialGuarantorNaturalPersonForm.getNoDocument()) {
            deleteFilesIfExistedBefore(document);
        }
        document.setNoDocument(documentFinancialGuarantorNaturalPersonForm.getNoDocument());
        documentRepository.save(document);

        if (Boolean.FALSE.equals(documentFinancialGuarantorNaturalPersonForm.getNoDocument())) {
            if (documentFinancialGuarantorNaturalPersonForm.getDocuments().size() > 0) {
                saveFiles(documentFinancialGuarantorNaturalPersonForm, document);
                document.setCustomText(null);
            } else {
                log.info("Refreshing info in [FINANCIAL] document with ID [" + documentFinancialGuarantorNaturalPersonForm.getDocumentId() + "]");
            }
        } else {
            document.setCustomText(documentFinancialGuarantorNaturalPersonForm.getCustomText());
        }
        documentRepository.save(document);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.FINANCIAL);
        if (tenant.getStatus() == TenantFileStatus.VALIDATED) {
            documentService.resetValidatedDocumentsStatusOfSpecifiedCategoriesToToProcess(guarantor.getDocuments(), List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.FINANCIAL, DocumentCategory.TAX));
        }
        tenantStatusService.updateTenantStatus(tenant);
        apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
        tenantRepository.save(tenant);
        return document;
    }

    private void deleteFilesIfExistedBefore(Document document) {
        documentHelperService.deleteFiles(document);
    }
}
