package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.FileNotFoundException;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.service.interfaces.DocumentIAService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.FileService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
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
    private final DocumentIAService documentIAService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Document delete(Long id, Tenant tenant) {
        File file = getFileForTenantOrCouple(id, tenant);

        Document document = file.getDocument();
        Tenant tenantFileOwner = document.getTenant();

        document.getFiles().remove(file);
        file.setDocument(null);
        fileRepository.delete(file);

        logService.saveDocumentEditedLog(document, tenantFileOwner, EditionType.DELETE);
        documentService.markDocumentAsEdited(document);

        if (document.getFiles().isEmpty()) {
            documentService.delete(document);
            return null;
        }

        documentService.changeDocumentStatus(document, DocumentStatus.TO_PROCESS);

        documentIAService.analyseDocument(document);
        producer.sendDocumentForPdfGeneration(document);
        return document;
    }

    // We use this method to allow the tenant to show and download the files uploaded by the other tenant in the couple
    public File getFileForTenantOrCouple(Long fileId, Tenant tenant) throws FileNotFoundException {

        if (tenant.getApartmentSharing().getApplicationType() == ApplicationType.COUPLE) {
            return fileRepository.findByIdForAppartmentSharing(fileId, tenant.getApartmentSharing().getId()).orElseThrow(() -> new FileNotFoundException(fileId));
        }
        return fileRepository.findByIdForTenant(fileId, tenant.getId()).orElseThrow(() -> new FileNotFoundException(fileId));
    }
}
