package fr.dossierfacile.api.front.service;

import fr.dossierfacile.api.front.exception.FileNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.FileService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final OvhService ovhService;
    private final FileRepository fileRepository;
    private final DocumentRepository documentRepository;
    private final AuthenticationFacade authenticationFacade;
    private final DocumentService documentService;

    @Override
    @Transactional
    public void delete(Long id) {
        Tenant tenant = authenticationFacade.getPrincipalAuthTenant();
        File file = fileRepository.findByIdAndTenant(id, tenant.getId()).orElseThrow(() -> new FileNotFoundException(id, tenant));

        documentService.updateOthersDocumentsStatus(tenant);

        Document document = file.getDocument();

        ovhService.delete(file.getPath());
        fileRepository.delete(file);
        fileRepository.flush();

        if (document.getFiles().isEmpty()) {
            documentService.delete(document.getId());
        } else {
            document.setDocumentStatus(DocumentStatus.TO_PROCESS);
            documentRepository.save(document);
            documentService.generatePdfByFilesOfDocument(document);
        }
    }
}
