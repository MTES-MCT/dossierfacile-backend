package fr.gouv.bo.service;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentDeniedOptions;
import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.repository.StorageFileRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.gouv.bo.amqp.Producer;
import fr.gouv.bo.dto.MessageDTO;
import fr.gouv.bo.exception.DocumentNotFoundException;
import fr.gouv.bo.repository.DocumentDeniedOptionsRepository;
import fr.gouv.bo.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final StorageFileRepository storageFileRepository;
    private final Producer producer;
    private final DocumentDeniedOptionsRepository documentDeniedOptionsRepository;

    public Document findDocumentById(Long documentId) {
        return documentRepository.findById(documentId).orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    public Document findDocumentByName(String documentName) {
        return documentRepository.findByName(documentName).orElseThrow(() -> new DocumentNotFoundException(documentName));
    }

    public Tenant deleteDocument(Long documentId) {
        Document document = findDocumentById(documentId);

        Tenant tenant;
        if (document.getGuarantor() != null) {
            tenant = document.getGuarantor().getTenant();
        } else {
            tenant = document.getTenant();
        }

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
        StorageFile watermarkFile = document.getWatermarkFile();
        document.setWatermarkFile(null);
        documentRepository.save(document);
        if (watermarkFile != null) {
            storageFileRepository.delete(watermarkFile);
        }
    }

    @Transactional
    public void regenerateFailedPdfDocumentsUsingButtonRequest() {
        synchronized (this) {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            List<Long> documents = documentRepository.findWithoutPDFToDate(oneHourAgo);
            log.info("Regenerate [{}] Failed PDF in all status", documents.size());

            documents.forEach(documentId -> {
                try {
                    producer.generatePdf(documentId);
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Something wrong on sleep !!! ");
                }
            });
        }
    }

    @Transactional
    public void updateDocumentWithDocumentDeniedReasons(DocumentDeniedReasons documentDeniedReasons, Long documentId) {
        documentRepository.updateDocumentWithDocumentDeniedReasons(documentDeniedReasons, documentId);
    }

    public List<DocumentDeniedOptions> findDocumentDeniedOptionsByDocumentSubCategoryAndDocumentUserType(DocumentSubCategory documentSubCategory, String tenantOrGuarantor) {
        return documentDeniedOptionsRepository.findAllByDocumentSubCategoryAndDocumentUserTypeOrderByCode(documentSubCategory, tenantOrGuarantor);
    }
}
