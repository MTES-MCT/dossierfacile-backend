package fr.dossierfacile.api.front.register.tenant;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.TenantModel;
import fr.dossierfacile.api.front.register.SaveStep;
import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentPdfGenerationLog;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.repository.DocumentPdfGenerationLogRepository;
import fr.dossierfacile.common.repository.TenantCommonRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class DocumentFinancial implements SaveStep<DocumentFinancialForm> {

    private final DocumentHelperService documentHelperService;
    private final TenantCommonRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final TenantMapper tenantMapper;
    private final DocumentService documentService;
    private final TenantStatusService tenantStatusService;
    private final Producer producer;
    private final ApartmentSharingService apartmentSharingService;
    private final DocumentPdfGenerationLogRepository documentPdfGenerationLogRepository;

    @Override
    @Transactional
    public TenantModel saveStep(Tenant tenant, DocumentFinancialForm documentFinancialForm) {
        Document document = saveDocument(tenant, documentFinancialForm);
        producer.generatePdf(document.getId(),
                documentPdfGenerationLogRepository.save(DocumentPdfGenerationLog.builder()
                        .documentId(document.getId())
                        .build()).getId());
        return tenantMapper.toTenantModel(document.getTenant());
    }

    private Document saveDocument(Tenant tenant, DocumentFinancialForm documentFinancialForm) {
        DocumentSubCategory documentSubCategory = documentFinancialForm.getTypeDocumentFinancial();
        Document document = documentRepository.findByDocumentCategoryAndTenantAndId(DocumentCategory.FINANCIAL, tenant, documentFinancialForm.getId())
                .orElse(Document.builder()
                        .documentCategory(DocumentCategory.FINANCIAL)
                        .tenant(tenant)
                        .build());
        document.setDocumentStatus(DocumentStatus.TO_PROCESS);
        document.setDocumentDeniedReasons(null);
        document.setDocumentSubCategory(documentSubCategory);
        document.setMonthlySum(documentFinancialForm.getMonthlySum());
        if (document.getNoDocument() != null && !document.getNoDocument() && documentFinancialForm.getNoDocument()) {
            deleteFilesIfExistedBefore(document);
        }
        document.setNoDocument(documentFinancialForm.getNoDocument());
        documentRepository.save(document);

        if (Boolean.FALSE.equals(documentFinancialForm.getNoDocument())) {
            if (documentFinancialForm.getDocuments().size() > 0) {
                documentFinancialForm.getDocuments().stream()
                        .filter(f -> !f.isEmpty())
                        .forEach(multipartFile -> documentService.addFile(multipartFile, document));
                document.setCustomText(null);
            } else {
                log.info("Refreshing info in [FINANCIAL] document with ID [" + documentFinancialForm.getId() + "]");
            }
        } else {
            document.setCustomText(documentFinancialForm.getCustomText());
        }
        documentRepository.save(document);
        documentService.initializeFieldsToProcessPdfGeneration(document);
        tenant.lastUpdateDateProfile(LocalDateTime.now(), DocumentCategory.FINANCIAL);
        if (tenant.getStatus() == TenantFileStatus.VALIDATED) {
            documentService.resetValidatedDocumentsStatusOfSpecifiedCategoriesToToProcess(tenant.getDocuments(), List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.FINANCIAL, DocumentCategory.TAX));
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
