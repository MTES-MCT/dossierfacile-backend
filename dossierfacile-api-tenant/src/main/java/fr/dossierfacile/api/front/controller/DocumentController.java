package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.service.interfaces.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/document")
public class DocumentController {
    private static final String FILE_NO_EXIST = "The file does not exist";
    private final DocumentService documentService;
    private final AuthenticationFacade authenticationFacade;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var tenant = authenticationFacade.getLoggedTenant();
        documentService.delete(id, tenant);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/resource/{documentName:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void getPdfWatermarkedAsByteArray(HttpServletResponse response, @PathVariable String documentName) {
        Document document = documentRepository.findFirstByName(documentName).orElseThrow(() -> new DocumentNotFoundException(documentName));

        try (InputStream in = fileStorageService.download(document.getWatermarkFile())) {
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            IOUtils.copy(in, response.getOutputStream());
        } catch (FileNotFoundException e) {
            log.error(FILE_NO_EXIST);
            response.setStatus(404);
        } catch (IOException e) {
            log.error("Cannot download file", e);
            response.setStatus(404);
        }
    }
}
