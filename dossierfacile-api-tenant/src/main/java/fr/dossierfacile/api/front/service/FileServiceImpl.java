package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.FileNotFoundException;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.FileService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.model.log.EditionType;
import fr.dossierfacile.common.service.interfaces.LogService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {
    private final FileRepository fileRepository;
    private final DocumentService documentService;
    private final LogService logService;
    private final Producer producer;

    @Override
    @Transactional
    public Document delete(Long id, Tenant tenant) {
        File file = fileRepository.findByIdForAppartmentSharing(id, tenant.getApartmentSharing().getId()).orElseThrow(() -> new FileNotFoundException(id, tenant));

        Document document = file.getDocument();
        document.getFiles().remove(file);
        fileRepository.delete(file);

        logService.saveDocumentEditedLog(document, tenant, EditionType.DELETE);
        documentService.markDocumentAsEdited(document);

        if (document.getFiles().isEmpty()) {
            documentService.delete(document);
            return null;
        }
        documentService.changeDocumentStatus(document, DocumentStatus.TO_PROCESS);

        producer.sendDocumentForAnalysis(document);
        // We do that in the MEP of blurry analysis to handle the documents sent again to analysis
        // it will be possible that some files doesn't have any blurry analysis if they were added before the MEP
        // So we process them to avoid blocking the analysis of the document.
        // TODO : remove this after a while
        document.getFiles().forEach(documentFile -> {
            if (documentFile.getBlurryFileAnalysis() == null) {
                producer.amqpAnalyseFile(documentFile.getId());
            }
        });
        producer.sendDocumentForPdfGeneration(document);

        return document;

    }
}
