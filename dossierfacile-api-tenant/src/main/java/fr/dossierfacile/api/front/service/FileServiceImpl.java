package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.FileNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.ApartmentSharingService;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.FileService;
import fr.dossierfacile.api.front.service.interfaces.TenantService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileStorageService fileStorageService;
    private final FileRepository fileRepository;
    private final DocumentRepository documentRepository;
    private final AuthenticationFacade authenticationFacade;
    private final DocumentService documentService;
    private final TenantService tenantService;
    private final ApartmentSharingService apartmentSharingService;

    @Override
    @Transactional
    public Document delete(Long id, Tenant tenant) {
        File file = fileRepository.findByIdAndTenant(id, tenant.getId()).orElseThrow(() -> new FileNotFoundException(id, tenant));

        documentService.resetValidatedDocumentsStatusToToProcess(tenant);

        Document document = file.getDocument();

        fileStorageService.delete(file.getPath());
        fileRepository.delete(file);
        document.getFiles().remove(file);

        if (document.getFiles().isEmpty()) {
            documentService.delete(document.getId(), tenant);
            return null;
        } else {
            document.setDocumentStatus(DocumentStatus.TO_PROCESS);
            document.setDocumentDeniedReasons(null);
            Document documentSaved = documentRepository.saveAndFlush(document);
            tenantService.updateTenantStatus(tenant);
            apartmentSharingService.resetDossierPdfGenerated(tenant.getApartmentSharing());
            return documentSaved;
        }
    }
}
