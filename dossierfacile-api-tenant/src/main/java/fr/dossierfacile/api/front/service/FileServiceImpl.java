package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.FileNotFoundException;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.FileService;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.model.log.EditionType;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import fr.dossierfacile.common.service.interfaces.LogService;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final DocumentService documentService;
    private final LogService logService;
    private final Producer producer;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Document delete(Long id, Tenant tenant) {
        File file = fileRepository.findByIdForAppartmentSharing(id, tenant.getApartmentSharing().getId())
                .orElseThrow(() -> new FileNotFoundException(id, tenant));

        Document document = file.getDocument();

        detachAnalyses(file, document);
        document.getFiles().remove(file);
        file.setDocument(null);
        fileRepository.delete(file);

        logService.saveDocumentEditedLog(document, tenant, EditionType.DELETE);
        documentService.markDocumentAsEdited(document);

        if (document.getFiles().isEmpty()) {
            documentService.delete(document);
            return null;
        }

        documentService.changeDocumentStatus(document, DocumentStatus.TO_PROCESS);
        producer.sendDocumentForAnalysis(document);
        producer.sendDocumentForPdfGeneration(document);
        return document;
    }

    private void detachAnalyses(File file, Document document) {
        Long fileId = file.getId();
        Long documentId = document != null ? document.getId() : null;

        // Blurry
        BlurryFileAnalysis blurry = file.getBlurryFileAnalysis();
        if (blurry != null && blurry.getFile() != null) {
            // Sauvegarde des IDs dans les champs data* si pas déjà positionnés
            if (hasValueNull(blurry.getDataFileId())) blurry.setDataFileId(fileId);
            if (hasValueNull(blurry.getDataDocumentId())) blurry.setDataDocumentId(documentId);
            blurry.setFile(null);
            entityManager.merge(blurry);
        }
    }

    private boolean hasValueNull(Object v) {
        return v == null;
    }
}
