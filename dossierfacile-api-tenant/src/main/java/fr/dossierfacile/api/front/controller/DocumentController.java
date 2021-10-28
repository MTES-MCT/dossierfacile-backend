package fr.dossierfacile.api.front.controller;

import fr.dossierfacile.api.front.exception.DocumentNotFoundException;
import fr.dossierfacile.api.front.repository.DocumentRepository;
import fr.dossierfacile.api.front.security.interfaces.AuthenticationFacade;
import fr.dossierfacile.api.front.service.interfaces.DocumentService;
import fr.dossierfacile.common.service.interfaces.OvhService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.openstack4j.model.storage.object.SwiftObject;
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
@Slf4j
@RequestMapping("/api/document")
public class DocumentController {
    private static final String FILE_NO_EXIST = "The file does not exist";
    private final DocumentService documentService;
    private final AuthenticationFacade authenticationFacade;
    private final DocumentRepository documentRepository;
    private final OvhService ovhService;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var tenant = authenticationFacade.getTenant(null);
        documentService.delete(id, tenant);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/resource/{documentName:.+}")
    public void getPdfWatermarkedAsByteArray(HttpServletResponse response, @PathVariable String documentName) {
        documentRepository.findFirstByName(documentName).orElseThrow(() -> new DocumentNotFoundException(documentName));
        SwiftObject object = ovhService.get(documentName);
        if (object != null) {
            try (InputStream in = object.download().getInputStream()) {
                response.setContentType(MediaType.APPLICATION_PDF_VALUE);
                IOUtils.copy(in, response.getOutputStream());
            } catch (final IOException e) {
                log.error(FILE_NO_EXIST);
                response.setStatus(404);
            }
        } else {
            log.error(FILE_NO_EXIST);
            response.setStatus(404);
        }
    }
}
