package fr.dossierfacile.api.front.partner.controller;

import fr.dossierfacile.api.front.amqp.Producer;
import fr.dossierfacile.api.front.exception.FileNotFoundException;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.api.front.service.interfaces.FileService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentPdfGenerationLog;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.repository.DocumentPdfGenerationLogRepository;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api-partner/tenant/{tenantId}/file")
@Slf4j
public class ApiPartnerFileController {
    private static final String FILE_NO_EXIST = "The file does not exist";
    private final FileService fileService;
    private final DocumentService documentService;
    private final Producer producer;
    private final AuthenticationFacade authenticationFacade;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final DocumentPdfGenerationLogRepository documentPdfGenerationLogRepository;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @PathVariable Long tenantId) {
        var tenant = authenticationFacade.getTenant(tenantId);
        Document document = fileService.delete(id, tenant);
        if (document != null) {
            documentService.initializeFieldsToProcessPdfGeneration(document);
            producer.generatePdf(document.getId(),
                    documentPdfGenerationLogRepository.save(DocumentPdfGenerationLog.builder()
                            .documentId(document.getId())
                            .build()).getId());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/resource/{id}", produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public void getPrivateFileAsByteArray(HttpServletResponse response, @PathVariable Long id, @PathVariable Long tenantId) {
        Tenant tenant = authenticationFacade.getTenant(tenantId);
        File file = fileRepository.findByIdForApartmentSharing(id, tenant.getApartmentSharing().getId()).orElseThrow(() -> new FileNotFoundException(id));

        try (InputStream in = fileStorageService.download(file) ) {
            response.setContentType(file.getComputedContentType());
            IOUtils.copy(in, response.getOutputStream());
        } catch (final IOException e) {
            log.error(FILE_NO_EXIST);
            response.setStatus(404);
        }

    }
}
