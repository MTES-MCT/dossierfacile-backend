package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.mapper.TenantMapper;
import fr.dossierfacile.api.front.model.tenant.AnalysisStatus;
import fr.dossierfacile.api.front.model.tenant.DocumentAnalysisReportModel;
import fr.dossierfacile.api.front.model.tenant.DocumentAnalysisStatusResponse;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Person;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.model.log.EditionType;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.repository.DocumentIAFileAnalysisRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.LogService;
import fr.dossierfacile.document.analysis.service.DocumentIAService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentAnalysisReportRepository documentAnalysisReportRepository;
    private final DocumentIAFileAnalysisRepository documentIAFileAnalysisRepository;
    private final FileStorageService fileStorageService;
    private final TenantStatusService tenantStatusService;
    private final ApartmentSharingService apartmentSharingService;
    private final DocumentHelperService documentHelperService;
    private final LogService logService;
    private final Producer producer;
    private final DocumentIAService documentIAService;
    private final TenantMapper tenantMapper;

    @Override
    @Transactional
    public void changeDocumentStatus(Document document, DocumentStatus newStatus) {
        document.setDocumentStatus(newStatus);

        if (newStatus == DocumentStatus.TO_PROCESS) {
            document.setDocumentDeniedReasons(null);
            Tenant tenantOfDocument = Optional.ofNullable(document.getTenant()).orElseGet(() -> document.getGuarantor().getTenant());

            List<DocumentCategory> categoriesToChange = List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.FINANCIAL, DocumentCategory.TAX);
            if (categoriesToChange.contains(document.getDocumentCategory())) {
                List<Document> documentList = Optional.<Person>ofNullable(document.getTenant()).orElse(document.getGuarantor()).getDocuments();
                resetValidatedOrInProgressDocumentsAccordingCategories(documentList, categoriesToChange);
            }
            documentRepository.save(document);
            tenantStatusService.updateTenantStatus(tenantOfDocument);
            apartmentSharingService.resetDossierPdfGenerated(tenantOfDocument.getApartmentSharing());
        } else {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    @Override
    @Transactional
    public void delete(Document document) {
        Person ownerOfDocument = Optional.<Person>ofNullable(document.getTenant()).orElse(document.getGuarantor());
        Tenant tenantOfDocument = Optional.ofNullable(document.getTenant()).orElseGet(() -> document.getGuarantor().getTenant());

        List<DocumentCategory> categoriesToChange = List.of(DocumentCategory.PROFESSIONAL, DocumentCategory.FINANCIAL, DocumentCategory.TAX);
        if (categoriesToChange.contains(document.getDocumentCategory())) {
            List<Document> documentList = ownerOfDocument.getDocuments();
            resetValidatedOrInProgressDocumentsAccordingCategories(documentList, categoriesToChange);
        }

        ownerOfDocument.getDocuments().removeIf(d -> Objects.equals(d.getId(), document.getId()));
        documentRepository.delete(document);
        tenantStatusService.updateTenantStatus(tenantOfDocument);
        apartmentSharingService.resetDossierPdfGenerated(tenantOfDocument.getApartmentSharing());
    }

    @Override
    @Transactional
    public void delete(Long documentId, Tenant referenceTenant) {
        Document document = documentRepository.findByIdForApartmentSharing(documentId, referenceTenant.getApartmentSharing().getId())
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        delete(document);
        logService.saveDocumentEditedLog(document, referenceTenant, EditionType.DELETE);
    }

    @Override
    @Transactional
    public void resetValidatedOrInProgressDocumentsAccordingCategories(List<Document> documentList, List<DocumentCategory> categoriesToChange) {
        Optional.ofNullable(documentList)
                .orElse(new ArrayList<>())
                .forEach(document -> {
                    if (document.getDocumentStatus() != null && document.getDocumentStatus() != DocumentStatus.DECLINED
                            && categoriesToChange.contains(document.getDocumentCategory())) {
                        if (document.getDocumentStatus() == DocumentStatus.VALIDATED) {
                            document.setDocumentStatus(DocumentStatus.TO_PROCESS);
                            document.setDocumentDeniedReasons(null);
                        }
                        if (Boolean.TRUE == document.getNoDocument() && document.getWatermarkFile() != null) {
                            fileStorageService.delete(document.getWatermarkFile());
                            document.setWatermarkFile(null);
                        }
                        documentRepository.save(document);
                        documentIAService.analyseDocument(document);
                        if (Boolean.TRUE == document.getNoDocument()) {
                            producer.sendDocumentForPdfGeneration(document);
                        }
                    }
                });
    }

    @Transactional
    @Override
    public void addFile(MultipartFile multipartFile, Document document) throws IOException {
        File file = documentHelperService.addFile(multipartFile, document);
        markDocumentAsEdited(document);
        producer.minifyFile(document.getId(), file.getId());
        producer.analyzeFile(document.getId(), file.getId());
        producer.amqpAnalyseFile(file.getId());
        documentIAService.sendForAnalysis(multipartFile, file, document);
    }

    @Override
    public void markDocumentAsEdited(Document document) {
        document.setLastModifiedDate(LocalDateTime.now());
        if (document.getWatermarkFile() != null) {
            fileStorageService.delete(document.getWatermarkFile());
            document.setWatermarkFile(null);
        }
        documentRepository.save(document);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentAnalysisStatusResponse getDocumentAnalysisStatus(Long documentId, Tenant tenant) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (!hasPermissionOnDocument(document, tenant)) {
            throw new AccessDeniedException("Access Denied");
        }

        Long totalFiles = documentIAFileAnalysisRepository.countTotalFilesByDocumentId(documentId);
        if (totalFiles == 0) {
            // NO_ANALYSIS_SCHEDULED scenario
            return DocumentAnalysisStatusResponse.builder()
                    .status(AnalysisStatus.NO_ANALYSIS_SCHEDULED)
                    .build();
        }

        Long analyzedFiles = documentIAFileAnalysisRepository.countAnalyzedFilesByDocumentId(documentId);

        if (analyzedFiles.equals(totalFiles)) {
            // All files analyzed - COMPLETED scenario
            Optional<DocumentAnalysisReport> reportOptional = documentAnalysisReportRepository.findByDocumentId(documentId);
            DocumentAnalysisReportModel reportModel = reportOptional
                    .map(tenantMapper::toDocumentAnalysisReportModel)
                    .orElse(null);

            return DocumentAnalysisStatusResponse.builder()
                    .status(AnalysisStatus.COMPLETED)
                    .analysisReport(reportModel)
                    .build();
        }
        else {
            // 6. IN_PROGRESS scenario
            return DocumentAnalysisStatusResponse.builder()
                    .status(AnalysisStatus.IN_PROGRESS)
                    .analyzedFiles(analyzedFiles.intValue())
                    .totalFiles(totalFiles.intValue())
                    .build();
        }
    }

    private boolean hasPermissionOnDocument(Document document, Tenant tenant) {
        Tenant documentTenant = resolveDocumentTenant(document);
        if (documentTenant == null) {
            return false;
        }

        // Document belongs to this tenant (directly or through one of their guarantors)
        if (Objects.equals(documentTenant.getId(), tenant.getId())) {
            return true;
        }

        // In COUPLE mode, partner's documents (and their guarantors') are accessible
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        if (apartmentSharing.getApplicationType() == ApplicationType.COUPLE) {
            return apartmentSharing.getTenants().stream()
                    .anyMatch(t -> Objects.equals(t.getId(), documentTenant.getId()));
        }

        return false;
    }

    /**
     * Resolves the tenant associated with a document:
     * - if the document belongs to a tenant, returns that tenant
     * - if the document belongs to a guarantor, returns the guarantor's tenant
     */
    private Tenant resolveDocumentTenant(Document document) {
        if (document.getTenant() != null) {
            return document.getTenant();
        }
        if (document.getGuarantor() != null) {
            return document.getGuarantor().getTenant();
        }
        return null;
    }


}
