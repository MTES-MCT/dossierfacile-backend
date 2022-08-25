package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import fr.dossierfacile.common.entity.DocumentPdfGenerationLog;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.DocumentPdfGenerationLogRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.gouv.bo.amqp.Producer;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.exception.DocumentNotFoundException;
import fr.gouv.bo.repository.DocumentDeniedOptionsRepository;
import fr.gouv.bo.repository.DocumentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    public final DocumentRepository documentRepository;
    public final FileStorageService fileStorageService;
    private final Producer producer;
    private final DocumentPdfGenerationLogRepository documentPdfGenerationLogRepository;
    private final DocumentDeniedOptionsRepository documentDeniedOptionsRepository;

    public Document findDocumentById(Long documentId) {
        return documentRepository.findById(documentId).orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    public Tenant deleteDocument(Long documentId) {
        Document document = findDocumentById(documentId);

        Tenant tenant;
        if (document.getGuarantor() != null) {
            tenant = document.getGuarantor().getTenant();
        } else {
            tenant = document.getTenant();
        }

        deleteFromStorage(document);
        documentRepository.delete(document);
        return tenant;
    }

    public void saveDocument(Document document) {
        documentRepository.save(document);
    }

    public Tenant changeStatusOfDocument(Long documentId, MessageDTO messageDTO) {
        Document document = findDocumentById(documentId);
        DocumentStatus documentStatus = DocumentStatus.valueOf(messageDTO.getMessage());

        document.setDocumentStatus(documentStatus);
        document.setDocumentDeniedReasons(null);
        documentRepository.save(document);

        Tenant tenant;
        if (document.getGuarantor() == null) {
            tenant = document.getTenant();
        } else {
            tenant = document.getGuarantor().getTenant();
        }

        return tenant;
    }

    public void initializeFieldsToProcessPdfGeneration(Document document) {
        document.setName(null);
        document.setProcessingStartTime(LocalDateTime.now());
        document.setProcessingEndTime(null);
        document.setRetries(0);
        document.setLocked(false);
        document.setLockedBy(null);
        documentRepository.save(document);
    }

    public void deleteFromStorage(Document document) {
        List<File> files = document.getFiles();
        if (files != null && !files.isEmpty()) {
            log.info("Removing files from storage of document with id [" + document.getId() + "]");
            fileStorageService.delete(files.stream().map(File::getPath).collect(Collectors.toList()));
        }
        if (document.getName() != null && !document.getName().isBlank()) {
            log.info("Removing document from storage with path [" + document.getName() + "]");
            fileStorageService.delete(document.getName());
        }
    }

    @Transactional
    public void regenerateFailedPdfDocumentsUsingButtonRequest() {
        synchronized (this) {
            documentRepository.unlockFailedPdfDocumentsGeneratedUsingButtonRequest();

            int numberOfUpdate = 1;
            int lengthOfPage = 1000;
            Pageable page = PageRequest.of(0, lengthOfPage, Sort.Direction.DESC, "id");
            LocalDateTime twelveHoursAgo = LocalDateTime.now().minusHours(12);
            Page<Long> documents = documentRepository.findAllFailedGeneratedPdfDocumentIdsSinceXTimeAgo(twelveHoursAgo, page);

            long totalElements = documents.getTotalElements();
            log.info("Number of documents to retry its PDF generation [" + totalElements + "]");
            try {
                while (!documents.isEmpty()) {
                    page = page.next();
                    documents.forEach(documentId -> producer.generatePdf(documentId,
                            documentPdfGenerationLogRepository.save(DocumentPdfGenerationLog.builder().documentId(documentId).build()).getId()));
                    log.info("Send number [" + numberOfUpdate++ + "] with " + documents.getNumberOfElements() + " documentId");
                    documents = documentRepository.findAllFailedGeneratedPdfDocumentIdsSinceXTimeAgo(twelveHoursAgo, page);
                    log.info("Waiting 5s for the next call...");
                    this.wait(5000);
                }
            } catch (InterruptedException e) {
                log.error("some exception message ", e);
                Thread.currentThread().interrupt();
            }
        }
    }


    @Transactional
    public void regenerateFailedPdfDocumentsOneDayAgoUsingScheduledTask() {
        synchronized (this) {
            documentRepository.unlockFailedPdfDocumentsGeneratedUsingScheduledTask();

            int numberOfUpdate = 1;
            int lengthOfPage = 1000;
            Pageable page = PageRequest.of(0, lengthOfPage, Sort.Direction.DESC, "id");
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            Page<Long> documents = documentRepository.findAllFailedGeneratedPdfDocumentIdsSinceXTimeAgo(oneDayAgo, page);

            long totalElements = documents.getTotalElements();
            log.info("Number of documents to retry its PDF generation [" + totalElements + "]");
            try {
                while (!documents.isEmpty()) {
                    page = page.next();
                    documents.forEach(documentId -> producer.generatePdf(documentId,
                            documentPdfGenerationLogRepository.save(DocumentPdfGenerationLog.builder().documentId(documentId).build()).getId()));
                    log.info("Send number [" + numberOfUpdate++ + "] with " + documents.getNumberOfElements() + " documentId");
                    documents = documentRepository.findAllFailedGeneratedPdfDocumentIdsSinceXTimeAgo(oneDayAgo, page);
                    log.info("Waiting 5s for the next call...");
                    this.wait(5000);
                }
            } catch (InterruptedException e) {
                log.error("InterruptedException regenerateFailedPdf ", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    @Transactional
    public void updateDocumentWithDocumentDeniedReasons(DocumentDeniedReasons documentDeniedReasons, Long documentId) {
        documentRepository.updateDocumentWithDocumentDeniedReasons(documentDeniedReasons, documentId);
    }

    public List<DocumentDeniedOptions> findDocumentDeniedOptionsByDocumentSubCategoryAndDocumentUserType(DocumentSubCategory documentSubCategory, String tenantOrGuarantor) {
        return documentDeniedOptionsRepository.findAllByDocumentSubCategoryAndDocumentUserType(documentSubCategory, tenantOrGuarantor);
    }
}
