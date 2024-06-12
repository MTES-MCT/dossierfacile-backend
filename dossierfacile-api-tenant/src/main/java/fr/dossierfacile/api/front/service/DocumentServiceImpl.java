package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.MinifyFileProducer;
import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.TenantStatusService;
import fr.dossierfacile.common.utils.TransactionalUtil;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Person;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.model.log.EditionType;
import fr.dossierfacile.common.repository.DocumentAnalysisReportRepository;
import fr.dossierfacile.common.service.interfaces.DocumentHelperService;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final FileStorageService fileStorageService;
    private final TenantStatusService tenantStatusService;
    private final ApartmentSharingService apartmentSharingService;
    private final DocumentHelperService documentHelperService;
    private final MinifyFileProducer minifyFileProducer;
    private final LogService logService;
    private final Producer producer;

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
                        if (document.getDocumentStatus() == DocumentStatus.VALIDATED){
                            document.setDocumentStatus(DocumentStatus.TO_PROCESS);
                            document.setDocumentDeniedReasons(null);
                        }
                        if (Boolean.TRUE == document.getNoDocument() && document.getWatermarkFile() != null) {
                            fileStorageService.delete(document.getWatermarkFile());
                            document.setWatermarkFile(null);
                        }
                        if (document.getDocumentAnalysisReport() != null) {
                            documentAnalysisReportRepository.delete(document.getDocumentAnalysisReport());
                            document.setDocumentAnalysisReport(null);
                        }
                        documentRepository.save(document);

                        TransactionalUtil.afterCommit(() -> {
                            producer.sendDocumentForAnalysis(document);// analysis should be relaunched for update rules
                            if (Boolean.TRUE == document.getNoDocument()){
                                producer.sendDocumentForPdfGeneration(document);
                            }
                        });
                    }
                });
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public void addFile(MultipartFile multipartFile, Document document) throws IOException {
        File file = documentHelperService.addFile(multipartFile, document);
        markDocumentAsEdited(document);

        TransactionalUtil.afterCommit(() -> {
            minifyFileProducer.minifyFile(file.getId());
            producer.analyzeFile(file);
        });
    }

    @Override
    public void markDocumentAsEdited(Document document) {
        document.setLastModifiedDate(LocalDateTime.now());
        if (document.getDocumentAnalysisReport() != null) {
            documentAnalysisReportRepository.delete(document.getDocumentAnalysisReport());
            document.setDocumentAnalysisReport(null);
        }
        if (document.getWatermarkFile() != null) {
            fileStorageService.delete(document.getWatermarkFile());
            document.setWatermarkFile(null);
        }
        documentRepository.save(document);
    }

}
