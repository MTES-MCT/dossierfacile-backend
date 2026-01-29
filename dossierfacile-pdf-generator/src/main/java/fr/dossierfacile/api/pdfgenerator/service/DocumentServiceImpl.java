package fr.dossierfacile.api.pdfgenerator.service;

import fr.dossierfacile.api.pdfgenerator.repository.DocumentRepository;
import fr.dossierfacile.api.pdfgenerator.service.interfaces.DocumentService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    @Override
    public boolean documentIsUpToDateAt(long timestamp, Long documentId) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        Document document = documentRepository.findById(documentId).orElse(null);

        return document != null && (document.getLastModifiedDate() == null || dateTime.isAfter(document.getLastModifiedDate()));
    }

    @Override
    public Document getDocument(Long documentId) {
        return documentRepository.findById(documentId).orElse(null);
    }

    @Transactional
    @Override
    public void saveWatermarkFileAt(long executionTimestamp, StorageFile watermarkFile, Long documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);

        // Handle case where document was deleted during processing
        if (document == null) {
            log.warn("Document {} was deleted during PDF generation, marking watermarkFile as TO_DELETE", documentId);
            fileStorageService.delete(watermarkFile);
            return;
        }

        if (documentIsUpToDateAt(executionTimestamp, document.getId())) {
            // Mark previous watermarkFile as TO_DELETE before replacing
            StorageFile previousWatermarkFile = document.getWatermarkFile();
            if (previousWatermarkFile != null) {
                fileStorageService.delete(previousWatermarkFile);
            }

            document.setWatermarkFile(watermarkFile);
            documentRepository.save(document);
            log.info("PDF Generation execution is a success for documentId={}", documentId);
        } else {
            fileStorageService.delete(watermarkFile);
            log.warn("Document {} was modified after PDF generation started, discarding outdated watermarkFile", documentId);
        }
    }
}
